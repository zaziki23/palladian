package ws.palladian.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import ws.palladian.helper.html.HTMLHelper;
import ws.palladian.helper.nlp.StringHelper;

// TODO Remove all functionalities that are provided by apache commons.
/**
 * The FileHelper helps with file concerning tasks. If you add methods to this class, make sure under all circumstances
 * that all streams are closed correctly. Every time you cause a memory leak, god kills a kitten!
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Martin Werner
 * @author Sandro Reichert
 */
public class FileHelper {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FileHelper.class);

    /** Constant for video file extensions. */
    private static final List<String> VIDEO_FILE_EXTENSIONS = Arrays.asList("mp4", "flv", "avi", "mpeg2", "divx", "mov", "xvid");

    /** Constant for audio file extensions. */
    private static final List<String> AUDIO_FILE_EXTENSIONS = Arrays.asList("mp3", "ogg", "aac", "wav", "flac");

    /**
     * Checks if is file name.
     * 
     * @param name the name
     * @return true, if is file name
     */
    public static boolean isFileName(String name) {
        name = name.trim();

        Pattern pattern = Pattern.compile("\\.[A-Za-z0-9]{2,5}$", Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(name);

        return m.find();
    }

    /**
     * Checks if is video file.
     *
     * @param fileType the file type
     * @return true, if is video file
     */
    public static boolean isVideoFile(String fileType) {
        return VIDEO_FILE_EXTENSIONS.contains(fileType.toLowerCase());
    }

    /**
     * Checks if is audio file.
     *
     * @param fileType the file type
     * @return true, if is audio file
     */
    public static boolean isAudioFile(String fileType) {
        return AUDIO_FILE_EXTENSIONS.contains(fileType.toLowerCase());
    }

    /**
     * Gets the file path.<br>
     * data/models/model1.ser => data/models/<br>
     * data/models/ => data/models/<br>
     * 
     * @param path The full path.
     * @return the The folder part of the path without the filename.
     */
    public static String getFilePath(String path) {
        String filePath = path;
        int lastDot = path.lastIndexOf(".");
        int lastSeparator = path.lastIndexOf("/") + 1;
        if (lastSeparator == 0) {
            lastSeparator = path.lastIndexOf("\\") + 1;
        }
        if (lastDot > -1) {
            filePath = path.substring(0, lastSeparator);
        }
        return filePath;
    }

    /**
     * Gets the file name.
     *
     * @param path the path
     * @return the file name
     */
    public static String getFileName(String path) {
        String fileName = path;
        int lastDot = path.lastIndexOf(".");
        int lastSeparator = path.lastIndexOf("/") + 1;
        if (lastSeparator == 0) {
            lastSeparator = path.lastIndexOf("\\") + 1;
        }
        if (lastDot > -1) {
            fileName = path.substring(lastSeparator, lastDot);
        }
        return fileName;
    }

    public static String getFolderName(String path) {
        String fileName = path;
        int lastSeparator = path.lastIndexOf("/") + 1;
        if (lastSeparator == 0) {
            lastSeparator = path.lastIndexOf("\\") + 1;
        }
        if (lastSeparator > -1) {
            fileName = path.substring(lastSeparator);
        }
        return fileName;
    }

    public static String appendToFileName(String filePath, String appendix) {
        return getFilePath(filePath) + getFileName(filePath) + appendix + "." + getFileType(filePath);
    }

    /**
     * Gets the file type.
     *
     * @param path the path
     * @return the file type
     */
    public static String getFileType(String path) {
        String fileType = "";

        int lastQM = path.indexOf("?");

        // find last dot before "?"
        int lastDot = path.lastIndexOf(".");

        if (lastQM > -1) {
            lastDot = path.substring(0, lastQM).lastIndexOf(".");
        }

        if (lastDot > -1) {
            fileType = path.substring(lastDot + 1, path.length());
        }

        // throw away everything after "?"
        lastQM = fileType.indexOf("?");
        if (lastQM > -1) {
            fileType = fileType.substring(0, lastQM);
        }

        return fileType;
    }

    /**
     * Read html file to string.
     *
     * @param path the path
     * @param stripTags the strip tags
     * @return the string
     */
    public static String readHTMLFileToString(String path, boolean stripTags) {

        String contents = readFileToString(path);

        if (stripTags) {
            contents = StringEscapeUtils.unescapeHtml(contents);
            contents = HTMLHelper.stripHTMLTags(contents, true, false, false, false); // TODO remove JS, CSS,
            // comments and merge?
            return contents;
        }

        return contents;
    }

    /**
     * Read file to string.
     *
     * @param path the path
     * @return the string
     */
    public static String readFileToString(String path) {
        File contentFile = new File(path);
        return readFileToString(contentFile);
    }

    /**
     * Mimic the "tail" command.
     * 
     * @param path The path of the file.
     * @param numberOfLines The number of lines from the end of the file that should be returned
     * @return A string with text lines from the specified file.
     */
    public static String tail(String path, int numberOfLines) {

        StringBuilder contents = new StringBuilder();
        BufferedReader reader = null;

        int totalNumberOfLines = getNumberOfLines(path);

        try {

            reader = new BufferedReader(new FileReader(path));

            String line = "";
            int lineCount = 0;
            do {
                lineCount++;
                line = reader.readLine();
                if (line == null) {
                    break;
                }

                if (totalNumberOfLines - numberOfLines < lineCount) {
                    contents.append(line).append("\n");
                }

            } while (line != null);

        } catch (FileNotFoundException e) {
            LOGGER.error(path + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(path + ", " + e.getMessage());
        } catch (OutOfMemoryError e) {
            LOGGER.error(path + ", " + e.getMessage());
        } finally {
            close(reader);
        }

        return contents.toString();

    }

    public static String readFileToString(File file) {

        StringBuilder contents = new StringBuilder();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(file));

            String line = "";
            do {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                contents.append(line).append("\n");
            } while (line != null);

        } catch (FileNotFoundException e) {
            LOGGER.error(file + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(file + ", " + e.getMessage());
        } catch (OutOfMemoryError e) {
            LOGGER.error(file + ", " + e.getMessage());
        } finally {
            close(reader);
        }

        return contents.toString();
    }

    /**
     * Create a list with each line of the given file as an element.
     * 
     * @param path The path of the file.
     * @return A list with the lines as elements.
     */
    public static List<String> readFileToArray(String path) {
        File contentFile = new File(path);
        return readFileToArray(contentFile);
    }

    /**
     * <p>
     * 
     * </p>.
     *
     * @param fileURL the file url
     * @return the list
     */
    public static List<String> readFileToArray(URL fileURL) {
        File contentFile = null;
        try {
            contentFile = new File(fileURL.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("File: "+fileURL+" was not accessable!");
        }
        return readFileToArray(contentFile);

    }

    /**
     * Create a list with each line of the given file as an element.
     *
     * @param contentFile the content file
     * @return A list with the lines as elements.
     */
    public static List<String> readFileToArray(File contentFile) {
        List<String> list = new ArrayList<String>();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(contentFile));

            String line = "";
            do {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                list.add(line);
            } while (line != null);

        } catch (FileNotFoundException e) {
            LOGGER.error(contentFile.getPath() + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(contentFile.getPath() + ", " + e.getMessage());
        } catch (OutOfMemoryError e) {
            LOGGER.error(contentFile.getPath() + ", " + e.getMessage());
        } finally {
            close(reader);
        }

        return list;
    }

    /**
     * Split the contents of a file into lines.
     * For example: a, b, c becomes
     * a
     * b
     * c
     * 
     * when the separator is ",".
     * 
     * @param inputFilePath The input file.
     * @param outputFilePath Where the transformed file should be saved.
     * @param separator The separator that is used to split.
     */
    public static void fileContentToLines(String inputFilePath, String outputFilePath, String separator) {
        String content = readFileToString(inputFilePath);
        String[] lines = content.split(separator);

        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append("\n");
        }

        writeToFile(outputFilePath, sb);
    }

    /**
     * Remove identical lines for the given input file and save it to the output file.
     * 
     * @param inputFilePath The input file.
     * @param outputFilePath Where the transformed file should be saved.
     */
    public static void removeDuplicateLines(String inputFilePath, String outputFilePath) {
        List<String> lines = readFileToArray(inputFilePath);

        Set<String> lineSet = new HashSet<String>();

        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (lineSet.add(line) || line.length() == 0) {
                sb.append(line).append("\n");
            }
        }

        writeToFile(outputFilePath, sb);
    }

    /**
     * Perform action on every line.
     *
     * @param filePath the file path
     * @param la the la
     * @return the int
     */
    public static int performActionOnEveryLine(String filePath, LineAction la) {
        int lineNumber = 1;
        FileReader reader = null;

        try {
            reader = new FileReader(filePath);
            lineNumber = performActionOnEveryLine(reader, la);
        } catch (FileNotFoundException e) {
            LOGGER.error(filePath + ", " + e.getMessage());
        } finally {
            close(reader);
        }

        return lineNumber;
    }

    public static int performActionOnEveryLine(Reader reader, LineAction la) {

        int lineNumber = 1;
        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(reader);

            String line = "";
            do {
                line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }

                la.performAction(line, lineNumber++);

            } while (line != null && la.looping);

        } catch (FileNotFoundException e) {
            LOGGER.error(reader + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(reader + ", " + e.getMessage());
        } catch (OutOfMemoryError e) {
            LOGGER.error(reader + ", " + e.getMessage());
        } finally {
            close(bufferedReader);
        }

        return lineNumber - 1;
    }

    /**
     * Write to file.
     * 
     * @param filePath the file path
     * @param string the string
     * @return false if any IOException occurred. It is likely that {@link string} has not been written to
     *         {@link filePath}.
     *         See error log for details (Exceptions)
     */
    //    public static boolean writeToFile(String filePath, StringBuilder string) {
    //        return writeToFile(filePath, string.toString());
    //    }

    public static int performActionOnEveryLineText(String text, LineAction la) {
        return performActionOnEveryLine(new StringReader(text), la);

        //        int lineNumber = 1;
        //
        //        StringReader in = new StringReader(text);
        //        BufferedReader br = new BufferedReader(in);
        //        try {
        //            String line = "";
        //            do {
        //                line = br.readLine();
        //                if (line == null) {
        //                    break;
        //                }
        //
        //                la.performAction(line, lineNumber++);
        //
        //            } while (line != null && la.looping);
        //
        //            in.close();
        //            br.close();
        //
        //        } catch (IOException e) {
        //            LOGGER.error(e.getMessage());
        //        }
        //
        //        return lineNumber - 1;
    }

    /**
     * Writes a Collection of Objects to a file. Each Object's {{@link #toString()} invocation represents a line.
     * 
     * @param filePath the file path
     * @param lines the lines
     * @author Philipp Katz
     * @author Sandro Reichert
     * @return false if any IOException occurred. It is likely that {@link string} has not been written to
     *         {@link filePath}. See error log for details (Exceptions).
     */
    public static boolean writeToFile(String filePath, Collection<?> lines) {

        boolean success = false;
        File file = new File(filePath);
        if (!file.exists() && file.getParent() != null) {
            new File(file.getParent()).mkdirs();
        }

        FileWriter writer = null;

        try {
            writer = new FileWriter(file);
            for (Object line : lines) {
                writer.write(line.toString());
                writer.write(System.getProperty("line.separator"));
            }
            success = true;
        } catch (IOException e) {
            LOGGER.error(filePath + ", " + e.getMessage());
        } finally {
            close(writer);
        }
        return success;
    }

    /**
     * Write text to a file. If the file path ends with "gz" or "gzip" the contents will be gzipped automatically.
     * 
     * @param filePath The file path where the contents should be saved to.
     * @param string The string to save.
     * @return <tt>False</tt> if any IOException occurred. It is likely that {@link string} has not been written to
     *         {@link filePath}. See error log for details (Exceptions)
     */
    public static boolean writeToFile(String filePath, CharSequence string) {

        String fileType = getFileType(filePath);
        if (fileType.equalsIgnoreCase("gz") || fileType.equalsIgnoreCase("gzip")) {
            return gzip(string, filePath);
        }

        boolean success = false;
        File file = new File(filePath);
        if (!file.exists() && file.getParent() != null) {
            new File(file.getParent()).mkdirs();
        }

        FileWriter writer = null;

        try {
            writer = new FileWriter(file);
            writer.write(string.toString());
            success = true;
        } catch (IOException e) {
            LOGGER.error(filePath + ", " + e.getMessage());
        } finally {
            close(writer);
        }

        return success;
    }

    /**
     * Add some text to a file.
     * 
     * TODO Attention -- when appending to files too big for memory this method will cause data loss. readFileToString
     * will read until memory runs out (catching OutOfMemoryError) and return the partially read content, appendToFile
     * then appends to the partial content and writes it back to disk. I have added the two methods
     * appendFile/prependFile which use buffers instead of reading the whole files in memory. -- Philipp, 2010-07-10.
     * 
     * @param filePath The path to the file.
     * @param string The text to append.
     * @param before If true, the text will be appended before all other content, if false it will be appended to the
     *            end of the file.
     */
    //    @Deprecated
    //    public static void appendToFile(String filePath, StringBuilder string, boolean before) {
    //        appendToFile(filePath, string.toString(), before);
    //    }

    /**
     * Append to file.
     *
     * @param filePath the file path
     * @param string the string
     * @param before the before
     */
    //    @Deprecated
    //    public static void appendToFile(String filePath, String string, boolean before) {
    //        try {
    //            String currentContent = readFileToString(filePath);
    //            FileWriter fileWriter = new FileWriter(filePath);
    //            if (before) {
    //                fileWriter.write(string);
    //            }
    //            fileWriter.write(currentContent);
    //            if (!before) {
    //                fileWriter.write(string);
    //            }
    //            fileWriter.flush();
    //            fileWriter.close();
    //        } catch (IOException e) {
    //            LOGGER.error(filePath + ", " + e.getMessage());
    //        }
    //    }

    /**
     * Appends (i. e. inserts a the end) a string to the specified File.
     *
     * @param filePath the file path
     * @param stringToAppend the string to append
     * @throws IOException Signals that an I/O exception has occurred.
     * @author Philipp Katz
     * @return
     */
    public static boolean appendFile(String filePath, CharSequence stringToAppend) {

        boolean success = false;
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filePath, true));
            writer.append(stringToAppend);
            success = true;
        } catch (IOException e) {
            LOGGER.error(e);
        } finally {
            close(writer);
        }
        return success;

    }

    /**
     * Appends a line to the specified text file if it does not already exist.
     * 
     * @param filePath the file path; file will be created if it does not exist
     * @param stringToAppend the string to append
     * @return
     */
    public static boolean appendLineIfNotPresent(String filePath, final CharSequence stringToAppend) {
        boolean added = false;
        final boolean[] add = new boolean[] { true };
        // if file exists already, check if it contains specified line
        if (fileExists(filePath)) {
            performActionOnEveryLine(filePath, new LineAction() {
                @Override
                public void performAction(String line, int lineNumber) {
                    if (line.equals(stringToAppend)) {
                        add[0] = false;
                        breakLineLoop();
                    }
                }
            });
        }
        if (add[0]) {
            added = appendFile(filePath, stringToAppend + "\n");
        }
        return added;
    }

    /**
     * Prepends (i. e. inserts a the beginning) a String to the specified File.
     * 
     * Inspired by http://stackoverflow.com/questions/2537944/prepend-lines-to-file-in-java
     *
     * @param filePath the file path
     * @param stringToPrepend the string to prepend
     * @throws IOException Signals that an I/O exception has occurred.
     * @author Philipp Katz
     * @return
     */
    public static boolean prependFile(String filePath, String stringToPrepend)  {

        boolean success = false;
        RandomAccessFile randomAccessFile = null;

        try {

            randomAccessFile = new RandomAccessFile(filePath, "rw");

            byte[] writeBuffer = stringToPrepend.getBytes();

            // buffer size must be at least the size of String which we prepend
            int bufferSize = Math.max(4096, writeBuffer.length);

            // positions for read/write within the file at each iteration
            long readPosition = 0;
            long writePosition = 0;

            // # of bytes to write during next iteration, or -1, if done
            int writeBytes = writeBuffer.length;

            do {

                byte[] readBuffer = new byte[bufferSize];

                // read chunk, starting at current position to the readBuffer
                randomAccessFile.seek(readPosition);
                int readBytes = randomAccessFile.read(readBuffer);

                // write chunk from the writeBuffer, starting at current position
                randomAccessFile.seek(writePosition);
                randomAccessFile.write(writeBuffer, 0, writeBytes);

                // set read data to the writeBuffer for next iteration
                writeBuffer = readBuffer;

                readPosition += bufferSize;
                writePosition += writeBytes;
                writeBytes = readBytes;

            } while (writeBytes != -1);

            success = true;

        } catch (IOException e) {
            LOGGER.error(e);
        } finally {
            close(randomAccessFile);
        }
        return success;
    }

    /**
     * Deserialize a serialized object. If the filepath ends with "gz" it is automatically decompressed. This generic
     * method does the cast for you, just deserialize to the appropriate type, like
     * <tt>Foo foo = FileHelper.deserialize("foo.ser");</tt>.
     * 
     * @param <T> type of the objects.
     * @param filePath the file path
     * @return the object
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deserialize(String filePath) {

        if (getFileType(filePath).equalsIgnoreCase("gz")) {
            return (T) deserializeCompressed(filePath);
        }

        ObjectInputStream in = null;
        T obj = null;

        try {
            in = new ObjectInputStream(new FileInputStream(filePath));
            obj = (T) in.readObject();
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } catch (ClassNotFoundException e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(in);
        }

        return obj;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deserializeCompressed(String filePath) {

        ObjectInputStream ois = null;
        T obj = null;

        try {
            ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(filePath)));
            obj = (T) ois.readObject();
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } catch (ClassNotFoundException e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(ois);
        }

        return obj;
    }

    //public static void serialize(Object obj, String filePath) {
    /**
     * Serialize a serializable object. If the path ends with ".gz" it is automatically saved using gzip compression.
     * 
     * @param obj the obj
     * @param filePath the file path
     */
    public static void serialize(Serializable obj, String filePath) {

        if (getFileType(filePath).equalsIgnoreCase("gz")) {
            serializeCompress(obj, filePath);
            return;
        }

        ObjectOutputStream out = null;
        try {

            File outputFile = new File(FileHelper.getFilePath(filePath));
            if (!outputFile.exists()) {
                outputFile.mkdirs();
            }

            out = new ObjectOutputStream(new FileOutputStream(filePath));
            out.writeObject(obj);
        } catch (IOException e) {
            LOGGER.error("could not serialize object, " + e.getMessage());
        } catch (OutOfMemoryError e) {
            LOGGER.error("could not serialize object, " + e.getMessage() + ", exiting now!");
            System.exit(1);
        } catch (Exception e) {
            LOGGER.error("could not serialize object, " + e.getMessage());
        } finally {
            close(out);
        }
    }

    public static void serializeCompress(Serializable obj, String filePath) {
        ObjectOutputStream out = null;
        try {

            File outputFile = new File(FileHelper.getFilePath(filePath));
            if (!outputFile.exists()) {
                outputFile.mkdirs();
            }

            out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(filePath)));
            out.writeObject(obj);
        } catch (IOException e) {
            LOGGER.error("could not serialize object to " + filePath + ", " + e.getMessage(), e);
        } catch (OutOfMemoryError e) {
            LOGGER.error("could not serialize object to " + filePath + ", " + e.getMessage() + ", exiting now!");
            System.exit(1);
        } catch (Exception e) {
            LOGGER.error("could not serialize object to " + filePath + ", " + e.getMessage());
        } finally {
            close(out);
        }
    }

    /**
     * Rename.
     *
     * @param inputFile the input file
     * @param newName the new name
     * @return the string
     */
    public static String rename(File inputFile, String newName) {
        String fullPath = inputFile.getAbsolutePath();

        String oldName = inputFile.getName().replaceAll("\\..*", "");
        String newPath = fullPath.replaceAll(StringHelper.escapeForRegularExpression(oldName) + "\\.",
                StringHelper.escapeForRegularExpression(newName) + ".");

        return newPath;
    }

    /**
     * Copy a file.
     * 
     * @param sourceFile The file to copy.
     * @param destinationFile The destination of the file.
     */
    public static void copyFile(String sourceFile, String destinationFile) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(new File(sourceFile));
            out = new FileOutputStream(new File(destinationFile));

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (FileNotFoundException ex) {
            LOGGER.error(ex.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(in, out);
        }

    }

    /**
     * Copy directory.
     *
     * @param srcPath the src path
     * @param dstPath the dst path
     */
    public static void copyDirectory(String srcPath, String dstPath) {
        copyDirectory(new File(srcPath), new File(dstPath));
    }

    /**
     * Copy directory.
     *
     * @param srcPath the src path
     * @param dstPath the dst path
     */
    public static void copyDirectory(File srcPath, File dstPath) {

        if (srcPath.isDirectory()) {

            if (!dstPath.exists()) {
                dstPath.mkdir();
            }

            String files[] = srcPath.list();

            for (String file : files) {
                copyDirectory(new File(srcPath, file), new File(dstPath, file));
            }
        } else {

            if (!srcPath.exists()) {
                LOGGER.warn("File or directory does not exist.");
                return;
            } else {

                try {
                    InputStream in = new FileInputStream(srcPath);
                    OutputStream out = new FileOutputStream(dstPath);
                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];

                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }

                    in.close();
                    out.close();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }

        // System.out.println("Directory copied.");
    }

    /**
     * Delete a file or a directory.
     * 
     * @param filename The name of the file or directory.
     * @param deleteNonEmptyDirectory If true, and filename is a directory, it will be deleted with all its contents.
     * @return <tt>True</tt> if the deletion was successful, <tt>false</tt> otherwise.
     */
    public static boolean delete(String filename, boolean deleteNonEmptyDirectory) {
        File f = new File(filename);

        if (!f.exists()) {
            LOGGER.error("file can not be deleted because it does not exist");
            return false;
        }

        if (!f.canWrite()) {
            LOGGER.error("file can not be deleted because of lack of permission");
            return false;
        }

        // if it is a directory, make sure it is empty
        if (f.isDirectory()) {
            String[] files = f.list();
            if (files.length > 0 && !deleteNonEmptyDirectory) {
                LOGGER.error("file can not be deleted because it is a non-empty directory");
                return false;
            } else {
                for (File directoryFile : f.listFiles()) {
                    // directoryFile.delete();
                    // changed this to work recursively -- Philipp, 2010-12-22.
                    delete(directoryFile.getPath(), true);
                }
            }
        }

        // attempt to delete it
        return f.delete();
    }

    /**
     * Delete.
     *
     * @param filename the filename
     * @return true, if successful
     */
    public static boolean delete(String filename) {
        return delete(filename, true);
    }

    /**
     * Delete all files inside a directory.
     *
     * @param dirPath the directoryPath
     * @return true, if successful
     */
    public static boolean cleanDirectory(String dirPath) {
        File file = new File(dirPath);
        boolean returnValue = false;

        if (file.isDirectory()) {
            String[] files = file.list();
            if (files.length > 0) {
                for (File directoryFile : file.listFiles()) {
                    directoryFile.delete();
                }
                returnValue = true;
            }
        }
        return returnValue;

    }


    /**
     * Move.
     *
     * @param file the file
     * @param newPath the new path
     * @return true, if successful
     */
    public static boolean move(File file, String newPath) {
        File newFile = new File(newPath);
        return file.renameTo(new File(newFile, file.getName()));
    }

    /**
     * Add a header to all files from a certain folder.
     * 
     * @param folderPath The path to the folder.
     * @param header The header text to append.
     */
    public static void addFileHeader(String folderPath, StringBuilder header) {
        File[] files = getFiles(folderPath);
        for (File file : files) {
            // FileHelper.appendToFile(file.getAbsolutePath(), header, true);
            appendFile(file.getAbsolutePath(), header + "\n");
        }
    }

    /**
     * Get all files from a certain folder.
     * 
     * @param folderPath The path to the folder.
     * @return An array of files that are in that folder.
     */
    public static File[] getFiles(String folderPath) {
        return getFiles(folderPath, "");
    }

    /**
     * Gets the files.
     *
     * @param folderPath the folder path
     * @param substring the substring
     * @return the files
     */
    public static File[] getFiles(String folderPath, String substring) {
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();

            if (substring.length() > 0) {
                ArrayList<File> matchingFiles = new ArrayList<File>();

                for (File file : files) {
                    if (file.isDirectory()) {
                        continue;
                    }
                    if (file.getName().indexOf(substring) > -1) {
                        matchingFiles.add(file);
                    }
                }

                return matchingFiles.toArray(new File[0]);
            } else {
                return files;
            }
        }

        return new File[0];
    }

    /**
     * Get the number of lines in an ASCII document.
     * 
     * @param fileName The name of the file.
     * @return The number of lines.
     */
    public static int getNumberOfLines(String fileName) {
        LineAction la = new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
            }
        };
        return FileHelper.performActionOnEveryLine(fileName, la);
    }

    public static void zipFiles(File[] files, String targetFilename) {

        try {
            byte[] buffer = new byte[1024];

            FileOutputStream fout = new FileOutputStream(targetFilename);
            ZipOutputStream zout = new ZipOutputStream(fout);

            for (File sourceFile : files) {

                LOGGER.debug("adding " + sourceFile + " to zip");

                FileInputStream fin = new FileInputStream(sourceFile);

                // add the zip entry
                zout.putNextEntry(new ZipEntry(sourceFile.getPath()));

                // now we write the file
                int length;
                while ((length = fin.read(buffer)) > 0) {
                    zout.write(buffer, 0, length);
                }

                zout.closeEntry();
                fin.close();
            }

            zout.close();

        } catch (IOException ioe) {
            LOGGER.error("error creating the zip, " + ioe);
        }
    }

    /**
     * Zip some text and save to a file. http://www.java2s.com/Tutorial/Java/0180__File/ZipafilewithGZIPOutputStream.htm
     * 
     * @param text The text to be zipped.
     * @param filenameOutput The name of the zipped file.
     * @return True if zipping and saving was successfully, false otherwise.
     */
    public static boolean gzip(CharSequence text, String filenameOutput) {

        try {
            FileOutputStream out = new FileOutputStream(filenameOutput);
            GZIPOutputStream zipout = new GZIPOutputStream(out);

            StringReader in = new StringReader(text.toString());
            int c = 0;
            while ((c = in.read()) != -1) {
                zipout.write((byte) c);
            }
            in.close();
            zipout.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Zip string.
     *
     * @param text the text
     * @return the string
     */
    public static String gzipString(String text) {
        StringOutputStream out = null;
        try {
            StringInputStream in = new StringInputStream(text);
            out = new StringOutputStream();
            GZIPOutputStream zipout = new GZIPOutputStream(out);

            int c = 0;
            while ((c = in.read()) != -1) {
                zipout.write((byte) c);
            }
            in.close();
            zipout.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return out.toString();
        }

        return out.toString();
    }

    /**
     * Unzip a file.
     * 
     * @param filenameInput The name of the zipped file.
     * @param filenameOutput The target name of the unzipped file.
     */
    public static void ungzipFile(String filenameInput, String filenameOutput) {
        String unzippedContent = ungzipFileToString(filenameInput);
        writeToFile(filenameOutput, unzippedContent);
    }

    /**
     * Unzip file.
     *
     * @param filenameInput the filename input
     */
    public static void ungzipFile(String filenameInput) {
        String unzippedContent = ungzipFileToString(filenameInput);
        String filenameOutput = getFilePath(filenameInput) + getFileName(filenameInput);
        writeToFile(filenameOutput, unzippedContent);
    }

    /**
     * Unzip file7z.
     *
     * @param filenameInput the filename input
     */
    public static void unzipFile7z(String filenameInput) {
        try {
            Process p = Runtime.getRuntime().exec("7z e " + filenameInput);
            InputStream in = p.getInputStream();
            while (in.read() != -1) {
                // keep waiting until all output is rendered
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Unzip file cmd.
     *
     * @param filenameInput the filename input
     * @param consoleCommand the console command
     */
    public static void unzipFileCmd(String filenameInput, String consoleCommand) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(consoleCommand + " " + filenameInput);
            InputStream in = p.getInputStream();
            while (in.read() != -1) {
                // keep waiting until all output is rendered
            }
            p.getInputStream().close();
            p.destroy();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Unzip a file and return the unzipped string.
     * 
     * @param filename The name of the zipped file.
     * @return The unzipped content of the file.
     */
    public static String ungzipFileToString(String filename) {
        InputStream in = null;
        try {
            in = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
            return "";
        }

        return ungzipInputStreamToString(in);
    }

    /**
     * Unzip a string.
     *
     * @param in the in
     * @return The unzipped string.
     */
    /*
     * public static String unzipString(String zippedString) { InputStream in = new StringInputStream(zippedString);
     * //return unzipInputStreamToString(in);
     * StringOutputStream out = new StringOutputStream(); try { GZIPInputStream zipin = new GZIPInputStream(in); int
     * chunkSize = 8192; byte[] buffer = new
     * byte[chunkSize]; int length; while ((length = zipin.read(buffer, 0, chunkSize)) != -1) { out.write(buffer, 0,
     * length); } out.close(); zipin.close(); }
     * catch (FileNotFoundException e) { e.printStackTrace(); } catch (IOException e) { e.printStackTrace(); } return
     * out.toString(); }
     */

    /**
     * Unzip a input stream to string.
     * 
     * @param in The input stream with the zipped content.
     * @return The unzipped string.
     */
    public static String ungzipInputStreamToString(InputStream in) {
        StringOutputStream out = new StringOutputStream();
        GZIPInputStream zipIn = null;
        try {
            zipIn = new GZIPInputStream(in);
            int chunkSize = 8192;
            byte[] buffer = new byte[chunkSize];

            int length;
            while ((length = zipIn.read(buffer, 0, chunkSize)) != -1) {
                out.write(buffer, 0, length);
            }
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(zipIn);
        }
        return out.toString();
    }

    public static boolean unzipFile(String filename) {

        int bufferSize = 1024;

        try {

            BufferedOutputStream dest = null;
            FileInputStream fis = new FileInputStream(filename);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                LOGGER.debug("extracting: " + entry);
                int count;
                byte data[] = new byte[bufferSize];

                // write the files to the disk
                FileOutputStream fos = new FileOutputStream(getFilePath(filename) + entry.getName());
                dest = new BufferedOutputStream(fos, bufferSize);
                while ((count = zis.read(data, 0, bufferSize)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.close();
            }
            zis.close();

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * File exists.
     *
     * @param filePath the file path
     * @return true, if successful
     */
    public static boolean fileExists(String filePath) {
        File file = new File(filePath);
        if (file.exists() && !file.isDirectory()) {
            return true;
        }
        return false;
    }

    /**
     * Check if specified directory exists.
     * 
     * @param directoryPath
     * @return
     */
    public static boolean directoryExists(String directoryPath) {
        File file = new File(directoryPath);
        return file.exists() && file.isDirectory();
    }

    public static boolean createDirectory(String directoryPath) {
        return new File(directoryPath).mkdir();
    }

    /**
     * Concatenates file2 to the end of file1.
     * 
     * @param file1 The first file.
     * @param file2 The file that is appended to the end of file1.
     * @return True, if concatenation worked, false otherwise.
     */
    public static boolean concatenateFiles(File file1, File file2) {
        boolean concatenated = false;
        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {

            fis = new FileInputStream(file2);
            fos = new FileOutputStream(file1, true);

            int c;
            while ((c = fis.read()) != -1) {
                fos.write(c);
            }

            concatenated = true;

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(fis, fos);
        }

        return concatenated;
    }

    private static void close(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    LOGGER.error(e);
                }
            }
        }
    }

    /**
     * The main method.
     *
     * @param a the arguments
     */
    public static void main(String[] a) {

        // FileHelper.fileContentToLines("data/a.TXT", "data/a.TXT", ",");
        // FileHelper.removeDuplicateLines("data/temp/feeds.txt", "data/temp/feeds_d.txt");
        System.exit(0);

        // //////////////////////// add license to every file //////////////////////////
        // FileHelper.copyDirectory("src/tud", "data/temp/src/tud");
        // StringBuilder sb = new StringBuilder();
        // sb.append("/*\n");
        // sb.append(" * Copyright 2010 TU Dresden\n");
        // sb.append(" * Licensed under the Apache License, Version 2.0 (the \"License\"); you may not\n");
        // sb.append(" * use this file except in compliance with the License. You may obtain a copy of\n");
        // sb.append(" * the License at\n");
        // sb.append(" *\n");
        // sb.append(" *     http://www.apache.org/licenses/LICENSE-2.0\n");
        // sb.append(" *\n");
        // sb.append(" * Unless required by applicable law or agreed to in writing, software\n");
        // sb.append(" * distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT\n");
        // sb.append(" * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n");
        // sb.append(" * See the License for the specific language governing permissions and\n");
        // sb.append(" * limitations under the License.\n");
        // sb.append(" */\n\n");
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/entity", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/page", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/page/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/qa", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/query", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/snippet", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/classification/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/control", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/entity", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/fact", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/object", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/qa", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/qa/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/snippet", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/extraction/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/gui", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/helper", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/helper/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/knowledge", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/multimedia", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/multimedia/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/news", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/normalization", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/normalization/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/persistence", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/persistence/test", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/reporting", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/tagging", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/web", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/web/datasetcrawler", sb);
        // FileHelper.addFileHeader("data/temp/src/tud/iir/web/test", sb);
        // System.exit(0);
        // ////////////////////////add license to every file //////////////////////////
        writeToFile("temp/test.txt", Arrays.asList(new String[] { "one", "two", "three", "four" }));
        System.exit(0);

        FileHelper.move(new File("abc.txt"), "data");
        System.exit(0);
        FileHelper.unzipFile7z("wpc_1262_20091020_0017_1.log.gz");
        // FileHelper.unzipFile7z("abc.log.gz");
        // FileHelper.unzipFile("abc.log.gz", "abc_unzipped.log");
        System.exit(0);

        FileHelper.gzip("abc -1 sdf sdjfosd fs- 12\\n-1\\abc", "test.txt.gz");

        String unzippedText = FileHelper.ungzipFileToString("test.txt.gz");
        System.out.println(unzippedText);

        String zippedString = FileHelper.gzipString("abc -1 def");
        System.out.println(zippedString);

        FileHelper.ungzipFile("test.txt.gz", "unzipped.txt");

        // System.out.println(FileHelper.unzipString(zippedString));

        System.exit(0);

        isFileName("asdsf sd fs. afjh jerk.");
        isFileName("abc.com");
        isFileName("all.html");
        isFileName("ab.ai");
        isFileName("  abasdf.mpeg2 ");

        System.out.println(rename(new File("data/test/sampleTextForTagging.txt"), "sampleTextForTagging_tagged"));

    }

}