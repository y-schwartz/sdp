package org.yschwartz.sdp.common.util;

import static org.yschwartz.sdp.common.config.Constants.*;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StringUtils {
    public static String getSeparator(int length, char separator) {
        return IntStream.range(0, length).boxed().map(x -> separator).map(String::valueOf).collect(Collectors.joining());
    }

    public static String getLogFileName(String containerName) {
        return containerName + LOG_FILE_SUFFIX;
    }

    public static String appendNewLine(String line) {
        return line + NEW_LINE;
    }

    public static String createContainerName(String functionName) {
        return functionName + CONTAINER_NAME_DELIMITER + getFormattedTimestamp(LocalDateTime.now());
    }

    public static String getTimestampFromLogFileName(String fileName) {
        return org.apache.commons.lang.StringUtils.substringBetween(fileName, CONTAINER_NAME_DELIMITER, LOG_FILE_SUFFIX);
    }

    public static String getFormattedTimestamp(LocalDateTime time) {
        return time.toString().replace(':', '-');
    }
}
