package mwagent.order;

import static mwagent.common.Config.getConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ExtractLog extends Order {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    public ExtractLog(JSONObject command) {
        super(command);
    }

    @Override
    public int execute() {
        try {
            String additionalParamsStr = commandVo.getAdditionalParams();

            JSONParser parser = new JSONParser();
            JSONObject params = (JSONObject) parser.parse(additionalParamsStr);

            String fileParam = (String) params.get("file");
            if (fileParam != null && !fileParam.trim().isEmpty()) {
                java.io.File f = new java.io.File(fileParam);
                commandVo.setTargetFilePath(f.getParent() + java.io.File.separator);
                commandVo.setTargetFileName(f.getName());
            }
            String fileFullName = getFileFullName();

            String start = (String) params.get("start");
            String end = (String) params.get("end");
            String dateRegex = (String) params.get("dateRegex");
            String abbreviatePrefix = (String) params.get("abbreviatePrefix");
            String charsetParam = (String) params.get("charset");
            String charset = (charsetParam != null && !charsetParam.trim().isEmpty()) ? charsetParam.trim() : "UTF-8";
            
            JSONArray keywordsArray = (JSONArray) params.get("keywords");
            String[] keywords = new String[0];
            if (keywordsArray != null) {
                keywords = new String[keywordsArray.size()];
                for (int i = 0; i < keywordsArray.size(); i++) {
                    keywords[i] = (String) keywordsArray.get(i);
                }
            }

            List<BlockResult> blocks = extract(fileFullName, start, end, dateRegex, abbreviatePrefix, charset, keywords);

            JSONArray resultArray = new JSONArray();
            for (BlockResult block : blocks) {
                JSONObject obj = new JSONObject();
                obj.put("text", block.text);
                obj.put("count", block.count);
                resultArray.add(obj);
            }

            String resultStr = resultArray.toString();
            String resultHash = getHash(resultStr);

            resultVo.setOk(true);
            resultVo.setHostName(commandVo.getHostName());
            resultVo.setTargetFilePath(commandVo.getTargetFilePath());
            resultVo.setTargetFileName(commandVo.getTargetFileName());
            
            if (!commandVo.getResultHash().isEmpty() && commandVo.getResultHash().equals(resultHash)) {
                resultVo.setResult("NO CHANGE");
            } else {
                resultVo.setResult(resultStr);
            }
            resultVo.setResultHash(resultHash);

        } catch (Exception e) {
            getConfig().getLogger().log(Level.WARNING, e.getMessage(), e);
            resultVo.setOk(false);
            resultVo.setHostName(commandVo.getHostName());
            resultVo.setTargetFilePath(commandVo.getTargetFilePath());
            resultVo.setTargetFileName(commandVo.getTargetFileName());
            resultVo.setResult("Error: " + e.getClass().getSimpleName());
        }

        return 1;
    }

    protected String getFileFullName() {
        String fileName = commandVo.getTargetFileName();
        String filePath = commandVo.getTargetFilePath();
        if (filePath != null && !filePath.endsWith("/") && !filePath.endsWith("\\")) {
            // Assume the separator is required if not present, though ReadPlainFile just concatenates.
            // But ReadPlainFile simply returns commandVo.getTargetFilePath() + file_name;
            return filePath + fileName;
        }
        return filePath + fileName;
    }

    public static class BlockResult {
        public final String text;
        public final int count;

        public BlockResult(String text, int count) {
            this.text = text;
            this.count = count;
        }
    }

    private static class Accumulator {
        String text;
        int count;

        Accumulator(String text) {
            this.text = text;
            this.count = 1;
        }
    }

    private List<BlockResult> extract(String filePath,
                                      String start,
                                      String end,
                                      String dateRegex,
                                      String abbreviatePrefix,
                                      String charset,
                                      String... keywords) throws IOException {

        LocalDateTime startTime = LocalDateTime.parse(start, TS_FORMAT);
        LocalDateTime endTime = LocalDateTime.parse(end, TS_FORMAT);
        Pattern tsPattern = Pattern.compile(dateRegex);

        Map<String, Accumulator> accByFirstLine = new LinkedHashMap<>();

        StringBuilder currentBlock = new StringBuilder();
        boolean inRange = false;
        String currentFirstLineContent = "";

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), java.nio.charset.Charset.forName(charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = tsPattern.matcher(line);
                LocalDateTime ts = null;
                if (m.lookingAt()) {
                    ts = tryParse(m.group(1));
                }

                if (ts != null) {
                    flushBlock(accByFirstLine, currentBlock, currentFirstLineContent, inRange, keywords, abbreviatePrefix);

                    currentBlock.setLength(0);
                    currentBlock.append(line);
                    currentFirstLineContent = line.substring(m.end()).trim();
                    inRange = !ts.isBefore(startTime) && !ts.isAfter(endTime);
                } else {
                    if (currentBlock.length() > 0) {
                        currentBlock.append(System.lineSeparator()).append(line);
                    }
                }
            }
            flushBlock(accByFirstLine, currentBlock, currentFirstLineContent, inRange, keywords, abbreviatePrefix);
        }

        List<BlockResult> results = new ArrayList<>();
        for (Accumulator acc : accByFirstLine.values()) {
            results.add(new BlockResult(acc.text, acc.count));
        }
        return results;
    }

    private LocalDateTime tryParse(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, TS_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    private void flushBlock(Map<String, Accumulator> accByFirstLine,
                            StringBuilder block,
                            String firstLineContent,
                            boolean inRange,
                            String[] keywords,
                            String abbreviatePrefix) {
        if (block.length() == 0 || !inRange) {
            return;
        }
        String text = block.toString();
        if (!containsAnyKeyword(text, keywords)) {
            return;
        }
        if (abbreviatePrefix != null && !abbreviatePrefix.isEmpty()) {
            text = abbreviateText(text, abbreviatePrefix);
        }

        Accumulator acc = accByFirstLine.get(firstLineContent);
        if (acc == null) {
            accByFirstLine.put(firstLineContent, new Accumulator(text));
        } else {
            acc.count++;
        }
    }

    private String abbreviateText(String text, String prefix) {
        String[] lines = text.split("\\r?\\n");
        if (lines.length <= 3) {
            return text;
        }

        StringBuilder sb = new StringBuilder();
        int consecutiveCount = 0;
        String firstAbbrevLine = null;
        String lastAbbrevLine = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith(prefix)) {
                if (consecutiveCount == 0) {
                    firstAbbrevLine = line;
                }
                lastAbbrevLine = line;
                consecutiveCount++;
            } else {
                if (consecutiveCount > 0) {
                    flushAbbreviation(sb, firstAbbrevLine, lastAbbrevLine, consecutiveCount);
                    consecutiveCount = 0;
                }
                sb.append(line).append(System.lineSeparator());
            }
        }
        if (consecutiveCount > 0) {
            flushAbbreviation(sb, firstAbbrevLine, lastAbbrevLine, consecutiveCount);
        }

        String result = sb.toString();
        if (result.endsWith(System.lineSeparator()) && !text.endsWith(System.lineSeparator())) {
            result = result.substring(0, result.length() - System.lineSeparator().length());
        }
        return result;
    }

    private void flushAbbreviation(StringBuilder sb, String firstLine, String lastLine, int count) {
        if (count == 1) {
            sb.append(firstLine).append(System.lineSeparator());
        } else if (count == 2) {
            sb.append(firstLine).append(System.lineSeparator());
            sb.append(lastLine).append(System.lineSeparator());
        } else {
            sb.append(firstLine).append(System.lineSeparator());
            sb.append("...").append(System.lineSeparator());
            sb.append(lastLine).append(System.lineSeparator());
        }
    }

    private boolean containsAnyKeyword(String text, String[] keywords) {
        if (keywords == null || keywords.length == 0) {
            return true;
        }
        for (String kw : keywords) {
            if (kw == null || kw.isEmpty()) {
                continue;
            }
            Pattern p = Pattern.compile(kw, Pattern.CASE_INSENSITIVE);
            if (p.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
}
