/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


//$Id$

package org.apache.oodt.cas.metadata.util;

//JDK imports
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//APACHE imports
import org.apache.commons.lang.StringUtils;

//OODT imports
import org.apache.oodt.cas.commons.date.DateUtils;
import org.apache.oodt.cas.commons.exec.EnvUtilities;
import org.apache.oodt.cas.metadata.Metadata;

/**
 * @author mattmann
 * @author bfoster
 * @version $Revision$
 * 
 * <p>
 * A Utility class for replacing environment variables and maniuplating file
 * path strings.
 * </p>.
 */
public final class PathUtils {

    public static String DELIMITER = ",";

    public static String replaceEnvVariables(String origPath) {
        return replaceEnvVariables(origPath, null);
    }

    public static String replaceEnvVariables(String origPath, Metadata metadata) {
        return replaceEnvVariables(origPath, metadata, false);
    }

    public static String replaceEnvVariables(String origPath,
            Metadata metadata, boolean expand) {
        StringBuffer finalPath = new StringBuffer();

        for (int i = 0; i < origPath.length(); i++) {
            if (origPath.charAt(i) == '[') {
                VarData data = readEnvVarName(origPath, i);
                String var = null;
                if (metadata != null
                        && metadata.getMetadata(data.getFieldName()) != null) {
                    List valList = metadata.getAllMetadata(data.getFieldName());
                    var = (String) valList.get(0);
                    if (expand)
                        for (int j = 1; j < valList.size(); j++)
                            var += DELIMITER + (String) valList.get(j);
                } else {
                    var = EnvUtilities.getEnv(data.getFieldName());
                }
                finalPath.append(var);
                i = data.getEndIdx();
            } else {
                finalPath.append(origPath.charAt(i));
            }
        }

        return finalPath.toString();
    }

    public static String doDynamicReplacement(String string) throws Exception {
        return doDynamicReplacement(string, null);
    }

    public static String doDynamicReplacement(String string, Metadata metadata)
            throws Exception {
        return PathUtils.replaceEnvVariables(doDynamicDateReplacement(
                   doDynamicDateFormatReplacement(
                       doDynamicUtcToTaiDateReplacement(
                           doDynamicDateToSecsReplacement(
                               doDynamicDateToMillisReplacement(
                                   string, metadata),
                               metadata),
                           metadata),
                       metadata), 
                   metadata), 
               metadata, true);
    }

    public static String doDynamicDateReplacement(String string,
            Metadata metadata) throws Exception {
        Pattern datePattern = Pattern
                .compile("\\[\\s*DATE\\s*(?:[+-]{1}[^\\.]{1,}?){0,1}\\.\\s*(?:(?:DAY)|(?:MONTH)|(?:YEAR)|(?:UTC)|(?:TAI)){1}\\s*\\]");
        Matcher dateMatcher = datePattern.matcher(string);
        while (dateMatcher.find()) {
            String dateString = string.substring(dateMatcher.start(),
                    dateMatcher.end());
            GregorianCalendar gc = new GregorianCalendar();

            // roll the date if roll days was specfied
            int plusMinusIndex;
            if ((plusMinusIndex = dateString.indexOf('-')) != -1
                    || (plusMinusIndex = dateString.indexOf('+')) != -1) {
                int dotIndex;
                if ((dotIndex = dateString.indexOf('.', plusMinusIndex)) != -1) {
                    int rollDays = Integer.parseInt(PathUtils
                            .replaceEnvVariables(
                                    dateString.substring(plusMinusIndex,
                                            dotIndex), metadata).replaceAll(
                                    "[\\+\\s]", ""));
                    gc.add(GregorianCalendar.DAY_OF_YEAR, rollDays);
                } else
                    throw new Exception(
                            "Malformed dynamic date replacement specified (no dot separator) - '"
                                    + dateString + "'");
            }

            // determine type and make the appropriate replacement
            String[] splitDate = dateString.split("\\.");
            if (splitDate.length < 2)
                throw new Exception("No date type specified - '" + dateString
                        + "'");
            String dateType = splitDate[1].replaceAll("[\\[\\]\\s]", "");
            String replacement = "";
            if (dateType.equals("DAY")) {
                replacement = StringUtils.leftPad(gc
                        .get(GregorianCalendar.DAY_OF_MONTH)
                        + "", 2);
            } else if (dateType.equals("MONTH")) {
                replacement = StringUtils.leftPad((gc
                        .get(GregorianCalendar.MONTH) + 1)
                        + "", 2);
            } else if (dateType.equals("YEAR")) {
                replacement = gc.get(GregorianCalendar.YEAR) + "";
            } else if (dateType.equals("UTC")) {
                replacement = DateUtils.toString(DateUtils.toUtc(gc));
            } else if (dateType.equals("TAI")) {
                replacement = DateUtils.toString(DateUtils.toTai(gc));
            } else {
                throw new Exception("Invalid date type specified '"
                        + dateString + "'");
            }

            string = StringUtils.replace(string, dateString, replacement);
            dateMatcher = datePattern.matcher(string);
        }
        return string;
    }

    public static String doDynamicDateFormatReplacement(String string,
            Metadata metadata) throws Exception {
        Pattern dateFormatPattern = Pattern
                .compile("\\[\\s*FORMAT\\s*\\(.{1,}?,.{1,}?,.{1,}?\\)\\s*\\]");
        Matcher dateFormatMatcher = dateFormatPattern.matcher(string);
        while (dateFormatMatcher.find()) {
            String dateFormatString = string.substring(dateFormatMatcher
                    .start(), dateFormatMatcher.end());

            // get arguments
            Matcher argMatcher = Pattern.compile("\\(.*\\)").matcher(
                    dateFormatString);
            argMatcher.find();
            String argsString = dateFormatString.substring(argMatcher.start() + 1,
                    argMatcher.end() - 1); 
            argsString = doDynamicReplacement(argsString, metadata);
            String[] args = argsString.split(",");
            String curFormat = args[0].trim();
            String dateString = args[1].trim();
            String newFormat = args[2].trim();

            // reformat date
            Date date = new SimpleDateFormat(curFormat).parse(dateString);
            String newDateString = new SimpleDateFormat(newFormat).format(date);

            // swap in date string
            string = StringUtils.replace(string, dateFormatString,
                    newDateString);
            dateFormatMatcher = dateFormatPattern.matcher(string);
        }

        return string;
    }
    
    /**
     * Replaces String method of format [UTC_TO_TAI(<utc-string format: "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'">)]
     * with TAI time with format: "yyyy-MM-dd'T'HH:mm:ss.SSS-0000<leapSecs>"
     */
    public static String doDynamicUtcToTaiDateReplacement(String string,
            Metadata metadata) throws Exception {
        Pattern utcToTaiPattern = Pattern.compile("\\[\\s*UTC_TO_TAI\\s*\\(.{1,}?\\)\\s*\\]");
        Matcher matcher = utcToTaiPattern.matcher(string);
        while (matcher.find()) {
            String utcToTaiString = string.substring(matcher.start(), matcher.end());
            Matcher argMatcher = Pattern.compile("\\(.*\\)").matcher(utcToTaiString);
            argMatcher.find();
            String utcDateString = 
                utcToTaiString.substring(argMatcher.start() + 1, argMatcher.end() - 1).trim();
            utcDateString = doDynamicReplacement(utcDateString, metadata);
            string = StringUtils.replace(string, utcToTaiString, 
                    DateUtils.toString(DateUtils.toTai(DateUtils.toCalendar(utcDateString, 
                            DateUtils.FormatType.UTC_FORMAT))));
            matcher = utcToTaiPattern.matcher(string);
        }
        return string;
    }
    
    /**
     * Replaces String method of format [DATE_TO_SECS(<date-string>,<DateUtils.FormatType>,<epoch-date format: "yyyy-MM-dd">)]
     * with seconds between <epoch-date> and <date-string> 
     */
    public static String doDynamicDateToSecsReplacement(String string,
            Metadata metadata) throws Exception {
        Pattern utcToTaiPattern = Pattern.compile("\\[\\s*DATE_TO_SECS\\s*\\(.{1,}?\\,.{1,}?,.{1,}?\\)\\s*\\]");
        Matcher matcher = utcToTaiPattern.matcher(string);
        while (matcher.find()) {
            String dateToSecsString = string.substring(matcher.start(), matcher.end());
            Matcher argMatcher = Pattern.compile("\\(.*\\)").matcher(dateToSecsString);
            argMatcher.find();
            String argsString = dateToSecsString.substring(argMatcher.start() + 1,
                    argMatcher.end() - 1);
            argsString = doDynamicReplacement(argsString, metadata);
            String[] args = argsString.split(",");
            String dateString = args[0].trim();
            String dateType = args[1].trim();
            String epochString = args[2].trim();
            Calendar date = DateUtils.toCalendar(dateString, DateUtils.FormatType.valueOf(dateType));
            Calendar epoch = DateUtils.toLocalCustomFormatCalendar(epochString, "yyyy-MM-dd");
            String seconds = DateUtils.toString(DateUtils.getTimeInSecs(date, epoch));
            string = StringUtils.replace(string, dateToSecsString, seconds);
            matcher = utcToTaiPattern.matcher(string);
        }
        return string;
    }
    
    /**
     * Replaces String method of format [DATE_TO_MILLIS(<date-string>,<DateUtils.FormatType>,<epoch-date format: "yyyy-MM-dd">)]
     * with milliseconds between <epoch-date> and <date-string> 
     */
    public static String doDynamicDateToMillisReplacement(String string,
            Metadata metadata) throws Exception {
        Pattern utcToTaiPattern = Pattern.compile("\\[\\s*DATE_TO_MILLIS\\s*\\(.{1,}?\\,.{1,}?,.{1,}?\\)\\s*\\]");
        Matcher matcher = utcToTaiPattern.matcher(string);
        while (matcher.find()) {
            String dateToMillisString = string.substring(matcher.start(), matcher.end());
            Matcher argMatcher = Pattern.compile("\\(.*\\)").matcher(dateToMillisString);
            argMatcher.find();
            String argsString = dateToMillisString.substring(argMatcher.start() + 1,
                    argMatcher.end() - 1);
            argsString = doDynamicReplacement(argsString, metadata);
            String[] args = argsString.split(",");
            String dateString = args[0].trim();
            String dateType = args[1].trim();
            String epochString = args[2].trim();
            Calendar date = DateUtils.toCalendar(dateString, DateUtils.FormatType.valueOf(dateType));
            Calendar epoch = DateUtils.toLocalCustomFormatCalendar(epochString, "yyyy-MM-dd");
            String milliseconds = DateUtils.getTimeInMillis(date, epoch) + "";
            string = StringUtils.replace(string, dateToMillisString, milliseconds);
            matcher = utcToTaiPattern.matcher(string);
        }
        return string;
    }

    private static VarData readEnvVarName(String origPathStr, int startIdx) {
        StringBuffer varName = new StringBuffer();
        int idx = startIdx + 1;

        do {
            varName.append(origPathStr.charAt(idx));
            idx++;
        } while (origPathStr.charAt(idx) != ']');

        VarData data = new PathUtils().new VarData();
        data.setFieldName(varName.toString());
        data.setEndIdx(idx);
        return data;

    }

    class VarData {

        private String fieldName = null;

        private int endIdx = -1;

        public VarData() {
        }

        /**
         * @return the endIdx
         */
        public int getEndIdx() {
            return endIdx;
        }

        /**
         * @param endIdx
         *            the endIdx to set
         */
        public void setEndIdx(int endIdx) {
            this.endIdx = endIdx;
        }

        /**
         * @return the fieldName
         */
        public String getFieldName() {
            return fieldName;
        }

        /**
         * @param fieldName
         *            the fieldName to set
         */
        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

    }

}