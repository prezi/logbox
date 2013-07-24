package com.prezi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {
    private static Pattern baseNamePattern = null;
    public static String baseName(String fileName) throws Exception {
        if ( baseNamePattern == null){
            baseNamePattern = Pattern.compile(".*/([^\\./]+)((\\.)([^\\./]+))?");

        }

        Matcher m = baseNamePattern.matcher(fileName);
        if (!m.matches()){
            throw new Exception(String.format("Can't calculate basename for file: %s", baseNamePattern));
        }

        return m.group(1);
    }

    public static String globToRegex(String glob) throws Exception {
        // Credits: Base algorithm from
        // http://stackoverflow.com/questions/1247772/is-there-an-equivalent-of-java-util-regex-for-glob-type-patterns
        String out = "^";
        Boolean in_braces = false;
        for(int i = 0; i < glob.length(); ++i)
        {
            final char c = glob.charAt(i);
            switch(c)
            {
                case '*': out += ".*";
                    break;;
                case '?': out += '.';
                    break;;
                case '.': out += "\\.";
                    break;;
                case '\\': out += "\\\\";
                    break;;
                case '{':
                    if (in_braces){
                        throw new Exception(String.format("Nested bracket glob expressions are not supported in globToRegex: %s", glob));
                    }
                    in_braces = true;
                    out += "((";
                    break;;
                case ',':
                    if (in_braces){
                        out += ")|(";
                    } else {
                        out += c;
                    }
                    break;;
                case '}':
                    if (!in_braces){
                        throw new Exception(String.format("Unexpected closing brace in globToRegex", glob));
                    }
                    out += "))";
                default:
                    out += c;
            }
        }
        out += '$';
        return out;
    }
}
