package tw.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;
import java.util.zip.ZipFile;

/** File related methods*/
public class FileUtilities {
//    
//    public enum DownloadCondition {
//        ALWAYS,
//        NEWER_REMOTE,
//        LOCAL_IN_FUTURE,
//        SIZE_DIFFERS
//    }
//    
//    // pre-defined download condition sets
//    public static final Set<DownloadCondition> FORCE = EnumSet.of(DownloadCondition.ALWAYS);
//    public static final Set<DownloadCondition> REMOTE_IS_NEWER = EnumSet.of(DownloadCondition.NEWER_REMOTE);
//    public static final Set<DownloadCondition> OUTDATED_OR_IN_FUTURE = EnumSet.of(DownloadCondition.NEWER_REMOTE, DownloadCondition.LOCAL_IN_FUTURE);
//    public static final Set<DownloadCondition> ANY_DIFFERENCE = EnumSet.of(DownloadCondition.NEWER_REMOTE, DownloadCondition.LOCAL_IN_FUTURE, DownloadCondition.SIZE_DIFFERS);
//    
//    // possible return values for downloadFileIfNewer
//    public static final int DF_NOT_NEEDED = 0;
//    public static final int DF_DOWNLOADED = 1;
//    private static final String FILE_ACCESS_MODE = "rw";
//    private static final long ONE_HOUR_IN_MILLIS = 3600000;
////    public static final int DF_ERROR = 2;
//    
//    public static FileStatus createSameThreadProgress() { return new SameThreadStatus(); }
//
//    /** the methods below download or copy a file and provide status updates to the downloadProgress object.
//     * the download/copy is done in place, so the facility does not provide any multithreading internally, it is up to the developer
//     * to multithread this call and handle callbacks accordingly. If no multithreading is needed, the developer should
//     * use createSameThreadProgress() to collect status information. The use of these calls in the AWT thread should be minimal
//     * because they may block if the connection times out */
//    public static void downloadOrCopyFileIfNewer( UrlCreator source, String destFilename, IFileProgress downloadProgress) {
//        downloadOrCopyFile(source, destFilename, downloadProgress, REMOTE_IS_NEWER);
//    }
//    
//    // alias for downloadOrCopyFileIfNewer
//    public static void downloadFileIfNewer( UrlCreator source, String destFilename, IFileProgress downloadProgress) {
//        downloadOrCopyFileIfNewer(source, destFilename, downloadProgress);
//    }
//    
//    /** @param sourceUrl could be URL or filename */
//    public static void downloadOrCopyFile( UrlCreator source, String destFilename, IFileProgress downloadProgress, Set<DownloadCondition> conditions) {
//        if (source.url().startsWith("http:") || source.url().startsWith("https:")) {
//            downloadFile(source, destFilename, downloadProgress, conditions);
//        }
//        else {
//            copyFile(source.url(), destFilename, downloadProgress, conditions);
//        }
//    }
//    
//    private static void downloadFile(UrlCreator sourceUrl, String destFilename, IFileProgress downloadProgress, Set<DownloadCondition> conditions) {
//        try {
//            File destFile = new File( destFilename );
//            boolean download = false;
//            boolean noInternet = SLogging.noInternet();
//                
//            if ( destFile.exists() && destFile.length() > 0 ) {
//                if ( noInternet ) {
//                    downloadProgress.finished(DF_NOT_NEEDED);
//                    SLogging.debug("no Internet - using already downloaded file " + destFilename);
//                }
//                else {
//                    download = true;
//                }
//            }
//            else if ( noInternet ) {
//                downloadProgress.error("No internet connectivity");
//                SLogging.log("cannot download file " + destFilename + "; no internet connectivity");
//            } 
//            else {
//                download = true;
//            }
//            
//            // copy file to local drive if it does not exist or web version is newer 
//            // local should be non 0 - this may happen if download aborts, ts will be wrong also
//            if ( download ) {
//                destFile.getParentFile().mkdirs();
//                destFile.createNewFile(); // may not be needed
//                getFileFromUrl(sourceUrl, destFilename, downloadProgress, conditions);
//            }
//        } 
//        catch (Exception e) {
//            String error = "can't load file ["+destFilename+"] from ["+sourceUrl+"]";
//            downloadProgress.error(error);
//            SLogging.err(error, e);
//        }
//    }
//    /** this downloads file without WWW_HB probed list generation: for connection is used only base Connection EndPoint */
//    public static void downloadFileIfNewer( String sourceUrl, String destFilename, IFileProgress downloadProgress) {
//        downloadFileIfNewer(sourceUrl, destFilename, downloadProgress, CheckHbSitesToken.DO_NOT_CHECK_HB_SITES);
//    }
//    
//    /** this downloads file with defined instruction: used WWW_HB probed list or NOT*/
//    public static void downloadFileIfNewer( String sourceUrl, String destFilename, IFileProgress downloadProgress, CheckHbSitesToken hbToken) {
//        downloadOrCopyFile(new UrlCreator(sourceUrl, hbToken), destFilename, downloadProgress, REMOTE_IS_NEWER);
//    }
//    
//    public static void copyFile( String sourceFilename, String destFilename, IFileProgress downloadProgress, Set<DownloadCondition> conditions) {
//        try {
//            File sourceFile = new File( sourceFilename);
//            File destFile = new File( destFilename );
//            destFile.getParentFile().mkdirs();
//            
//            if (needToUpdate(destFile, conditions, sourceFile.lastModified(), sourceFile.length())) {
//                if( !SLogging.copyFile( sourceFilename, destFilename) ) {
//                    downloadProgress.error("Could not copy file from " + sourceFilename + " to " +  destFilename);
//                }
//                else{
//                    downloadProgress.finished(DF_DOWNLOADED);
//                }
//            }
//            else{
//                downloadProgress.finished(DF_NOT_NEEDED);
//            }
//        } 
//        catch (Exception e) {
//            String error = "Error coping file ["+destFilename+"] from ["+sourceFilename+"]";
//            downloadProgress.error(error);
//            SLogging.err(error, e);
//        }
//    }
//
//    static boolean needToUpdate(File destFile, Set<DownloadCondition> conditions, long sourceLastModified, long sourceSize) {
//        if (!destFile.exists()) {
//            SLogging.err("getFileFromUrl: dest=%s new", destFile.getPath());
//            return true;
//        }
//        
//        long localLastModified = destFile.lastModified();
//        long localSize = destFile.length();
//        
//        if (localSize == 0 && sourceSize > 0) { // if file was missing, empty file is already created by this time (for HTTP, but not for local copy)
//            SLogging.err("getFileFromUrl: dest=%s empty sourceSize=%s", destFile.getPath(), sourceSize);
//            return true;
//        }
//        
//        boolean update = conditions.contains(DownloadCondition.ALWAYS) ||
//            (conditions.contains(DownloadCondition.NEWER_REMOTE) && (sourceLastModified > localLastModified || sourceLastModified == 0)) ||
//            (conditions.contains(DownloadCondition.LOCAL_IN_FUTURE) && isFileModifiedInFuture(localLastModified)) ||
//            (conditions.contains(DownloadCondition.SIZE_DIFFERS) && localSize != sourceSize && sourceSize > 0);
//
//        SLogging.err(JtsLocale.LOG_PREFIX + "needToUpdate  dest=%s  sourceMod=%s  destMod=%s  sourceSize=%s  destSize=%s  conditions=%s  download=%s",
//            destFile.getPath(), fmt( sourceLastModified), fmt( localLastModified), sourceSize, localSize, conditions, update);
//        
//        return update;
//    }
//    
//    private static String fmt(long time) {
//        SimpleDateFormat f = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");
//        return f.format( new Date( time) );
//    }
//
//    /** deletes if the file is empty and returns true otherwise returns false */
//    public static boolean deleteFileIfEmpty(String fileLoc){
//        File file = new File(fileLoc);
//        return deleteFileIfEmpty(file);
//    }
//
//    public static boolean deleteFileIfEmpty(File file) {
//        if (file.exists() && file.length() == 0) {
//            file.delete();
//            return true;
//        }
//        return false;
//    }
//
//    /** downloads resource for defined UrlCreator. UrlCreator contains instruction: 
//     *  use WWW WWW_HB list or not. See CheckHbSitesToken
//     *  iteration stops when next probed from WWW_HB list succeeded 
//     *  @param lastModified - if > 0, then check timestamp before download and skip when older  
//     *  @return HTTP Status code HTTP_OK, HTTP_NOT_MODIFIED, HTTP_UNAVAILABLE or -1 on error */
//    private static int getFileFromUrl( UrlCreator url, String dest, IFileProgress downloadProgress, Set<DownloadCondition> conditions) {
//        int hbListSize = url.hbListSize();
//        int rv = HttpURLConnection.HTTP_OK;
//        for (int i = 0; i < hbListSize; i++) {
//            if ( (rv = getFileFromUrl(url, dest, downloadProgress, i, conditions)) > 0 ) { // download done or not required
//                break;
//            }
//        }
//        return rv;
//    }
//    
//    /** @return HTTP Status code HTTP_OK, HTTP_NOT_MODIFIED, HTTP_UNAVAILABLE or -1 on error */
//    public static int getFileFromUrl( UrlCreator url, String dest, IFileProgress downloadProgress) {
//        return getFileFromUrl(url, dest, downloadProgress, FORCE);
//    }
//    
//    /** returns true if the file has a modification time more than 1 hour into the future*/
//    private static boolean isFileModifiedInFuture(long modifiedTime) {
//        if (modifiedTime == 0) {
//            return false;
//        }
//        return modifiedTime - System.currentTimeMillis() > ONE_HOUR_IN_MILLIS;
//    }
//    
//    /** @return HTTP Status code HTTP_OK, HTTP_NOT_MODIFIED, HTTP_UNAVAILABLE or -1 on error */
//    private static int getFileFromUrl( UrlCreator url, String dest, IFileProgress downloadProgress, int index, Set<DownloadCondition> conditions) {
//        // this function copies a file from the url to the dest
//        if (SLogging.noInternet()) {
//            downloadProgress.error("No internet connectivity");
//            return HttpURLConnection.HTTP_UNAVAILABLE; // no more processing needed
//        }
//        
//        int done = HttpURLConnection.HTTP_OK;
//        HttpURLConnection con = null;   
//        
//        String tempFilename = dest + SLogging.TEP_POSFIX;
//        try {
//            StringBuilder sb = new StringBuilder("Starting to download '").append(url.url()).append("'");
//            sb.append(" using HotBackup '").append(url.hbConnection(index).host()).append("'");
//            SLogging.log(sb.toString());
//            downloadProgress.start();
//            
//            // connect to URL
//            con = url.openConnection(index);
//            con.setInstanceFollowRedirects( false );
//
//            long remoteLength = con.getContentLength(); // NOTE: returns int!
//            long remoteLastModified = con.getLastModified();
//            File destFile = new File(dest);
//            
//            if (!needToUpdate(destFile, conditions, remoteLastModified, remoteLength)) {
//                downloadProgress.finished(DF_NOT_NEEDED);
//                return HttpURLConnection.HTTP_NOT_MODIFIED;
//            }
//
//            SLogging.debug(JtsLocale.LOG_PREFIX + "downloading file " + dest + " from " + url.url());
//            
//            InputStream is = null;
//            FileOutputStream os = null;
//            
//            try { 
//                if(con.getResponseCode() != HttpURLConnection.HTTP_OK) {
//                    throw new java.io.FileNotFoundException("URL: " + url.getPath() + " doesn't exist");
//                }
//                is = con.getInputStream();
//                // save to a temporary file first
//                os = new FileOutputStream( tempFilename);
//                // loop through data
//                boolean aborted = false;
//                byte[] bytes = new byte[4096];
//                int bytesRead = is.read( bytes);
//                long totalBytesRead = 0;
//                
//                while( bytesRead > 0) {
//                    os.write( bytes, 0, bytesRead);
//                    bytesRead = is.read( bytes);
//                    totalBytesRead += bytesRead;
//                    try {
//                        downloadProgress.downloaded(totalBytesRead, remoteLength);
//                    }
//                    catch(Exception e){
//                        SLogging.err("Error in progress");
//                    }
//                    if (downloadProgress.isCancelled()) {
//                       aborted = true;
//                       break;
//                    }
//                }
//                
//                try { // close FileOutputStream and InputStream before file renaming / deleting
//                    os.close();
//                    os = null;
//                } catch (IOException e) {
//                    SLogging.anticipatedErr("Can't close", e);
//                }
//                try {
//                    is.close();
//                    is = null;
//                } catch (IOException e) {
//                    SLogging.anticipatedErr("Can't close", e);
//                }
//                
//                if ( aborted ) {
//                    deleteFile(tempFilename);
//                    downloadProgress.cancelled();
//                    SLogging.log("Canceled file downlaod: " + url.getPath());
//                } 
//                else {
//                    SLogging.log("Finished downloading file: " + url.getPath());
//                    boolean ret = renameFile(tempFilename, dest);
//                    if( ret ) {
//                        if ( remoteLastModified > 0 ) {
//                            destFile.setLastModified(remoteLastModified); // Set correct server-side timestamp to avoid dealing with local time
//                        }
//                        downloadProgress.finished(DF_DOWNLOADED);
//                    }
//                    else {
//                        downloadProgress.error("Rename from " + tempFilename + " to " + dest + " failed ");
//                    }
//                }
//            }
//            finally { // always close output file and input stream, since in one of catch is executing File.delete() 
//                if (os != null) {
//                    try {
//                        os.close();
//                    } catch (IOException e) {
//                        SLogging.anticipatedErr("Can't close", e);
//                    }
//                }
//                if (is != null) {
//                    try {
//                        is.close();
//                    } catch (IOException e) {
//                        SLogging.anticipatedErr("Can't close", e);
//                    }
//                }
//            }
//        }
//        catch ( java.io.FileNotFoundException e ) {
//            SLogging.err("Cannot find " + url.url());
//            downloadProgress.error("Cannot find " + url.url() + "\n" + e.getMessage());
//            done = -1;
//        } 
//        catch( Exception e) {
//            StringBuilder sb = new StringBuilder("Failed to download '");
//            sb.append(SLogging.notNull(con != null ? con.getURL() : url.hbConnection(index))).append("'");
//            if (e instanceof UnknownHostException) {
//                sb.append(" due Unknown Host '").append(e.getMessage()).append("'");
//            } else {
//                sb.append(". Reason '").append(e.getMessage()).append("'");
//            }
//            SLogging.log(sb.toString());
//            downloadProgress.error("Unknown error:\n"+e.getMessage());
//            deleteFile(tempFilename);
//            done = -1;
//        } finally {
//            closeSilently(con);
//        }
//        return done;
//    }
//
//    static public boolean createDir( String str) {
//        // this function creates a directory
//    
//        File file = new File( str);
//        if( file.exists() && file.isDirectory() ) {
//            return true;
//        }
//        return file.mkdirs();
//    }
//
    static public boolean exists( String str) {
        // this function returns true if a file or
        // directory with the specified name exists
        return new File( str).exists();
    }
    
    public static String append(File f1, String f2) {
		return append( f1.getAbsolutePath(), f2);
	}
    
    static public String append(String s1, String s2) {
		return s1.length() > 0 && s1.charAt( s1.length() - 1) == '\\'
				? (s1 + s2) : (s1 + "\\" + s2);
	}

	
//
//    static public boolean deleteFile( String str) {
//        // this functions deletes the specified file
//    
//        File file = new File( str);
//        if(file.exists()){
//            return file.delete();
//        }
//        return false;
//    }
//
//    static public boolean deleteFileIfOlder( String str, long dateInMls) {
//        // this functions deletes the specified file if the file is older than the past in date
//    
//        File file = new File( str);
//        if(file.lastModified() < dateInMls){
//            return file.delete();
//        }
//        return false;
//    }
//
//    static public boolean renameFile( String fromName, String toName) {
//        // this functions renames the specified file, with delete if necessary
//        // if file toName already exists
//    
//        File toFile = new File(toName);
//        toFile.delete();
//        File fromFile = new File( fromName);
//        boolean res = fromFile.renameTo(toFile); // sometimes java may prohibit renaming, but will allow to overwrite the file
//        if (!res) {
//            SLogging.log("Direct rename failed, try to overwrite the file");
//            res = SLogging.copyFile(fromName, toName);
//            if (res) {
//                new File(fromName).delete(); // if simulated rename succeeded, delete the source
//            }
//        }
//        return res;
//    }
//    
//    public static String getSomeFilename( String ext, String day, String userDir) {    
//        // this function creates a filename that in the form
//        // <day>.<ext>, where <day> is the first three
//        // letters of a day; if the file exists and
//        // is more than 24 hours old, it is deleted before
//        // the name is returned
//        // *.trd should not be deleted here because they check date and deleted
//        // when trying to read from *.trd   
//        final int MAX_TIME   = 24 * 60 * 60 * 1000;  // # of ms's in 24 hours
//
//        // construct filename
//        String filename = SLogging.concat( userDir, day ) + "." + ext;
//
//        // delete the file if it's more than a week old
//        File file = new File( filename);
//        if( file.exists() ) {
//            // get calendar
//            Calendar calendar = Calendar.getInstance();
//            // compare now to last mod time
//            if( !("trd".equalsIgnoreCase(ext)) && calendar.getTime().getTime() - file.lastModified() > MAX_TIME) {
//                if (!file.delete()) {
//                    SLogging.warning("Cannot rotate the file:" + file.getAbsolutePath());
//                }
//            }
//        }
//        return filename;
//    }    
//    
//    public static void closeSilently(Closeable ioObj) {
//        // close the file
//        if (ioObj != null) {
//            try {
//                ioObj.close();
//            } catch (Exception ce) { /* safe */ }
//        }         
//    }
//    
//    public static void closeSilently(ZipFile zipFile) { // ZipFile doesn't implement Closeable
//        // close the file
//        if (zipFile != null) {
//            try {
//                zipFile.close();
//            } catch (Exception ce) { /* safe */ }
//        }
//    }
//    
//    public static void closeSilently(HttpURLConnection conn) {
//        // close the connection
//        if (conn != null) {
//            conn.disconnect();
//        }
//    }
//    
//    /** @return last modified time for resource based on UrlCreator. 
//     *  UrlCreator contains instruction: use WWW_HB list or not. See CheckHbSitesToken
//     *  iteration stops when next probed from WWW_HB list succeeded
//     *  To be sure that next WWW_HB connection EndPoint is OK it just opening and closing InputStream */    
//    public static long lastModified(UrlCreator fileUTL) {
//        if (!SLogging.noInternet()) {
//            int hbListSize = fileUTL.hbListSize();
//            for (int i = 0; i < hbListSize; i++) {
//                HttpURLConnection connection = null;
//                try {
//                    connection = fileUTL.openConnection(i);
//                    connection.setRequestMethod("HEAD"); // minimize load on server by just doing a head request
//                    InputStream is = connection.getInputStream(); // this allows to understand that host/resource is reachable or not
//                    is.close(); // just close
//                    return connection.getLastModified();
//                // do not dump stack when some "normal" network exceptions
//                } catch (UnknownHostException uhe) {
//                    SLogging.err("UnknownHostException - Can't get File time stamp for " + SLogging.notNull(connection != null ? connection.getURL() : fileUTL.getPath()));
//                } catch (SocketException see) {
//                    SLogging.err("SocketException - Can't get File time stamp for " + SLogging.notNull(connection != null ? connection.getURL() : fileUTL.getPath()));
//                } catch(Exception e) {                    
//                    SLogging.anticipatedErr("Can't get File time stamp for " + SLogging.notNull(connection != null ? connection.getURL() : fileUTL.getPath()), e);
//                }
//            }
//        } 
//        else {
//            SLogging.err("No internet connectivity, cannot check timestamp for " + fileUTL.getPath());
//        }
//        return 0;
//    }
//
//    public static String getExtension( String f ) {
//        int periodPos = f.lastIndexOf( '.' );
//        return periodPos >= 0 ? f.substring( periodPos + 1 ) : "";
//    }
//
//    public static String getExtension( File f ) {
//        return getExtension( f.getName() );
//    }
//
//    public static String getBaseName( String f ) {
//        return getBaseName( new File( f ) );
//    }
//
//    public static String getBaseName( File f ) {
//        String fileName = f.getName();
//        int periodPos = fileName.lastIndexOf( '.' );
//        return periodPos >= 0 ? fileName.substring( 0, periodPos ) : fileName;
//    }
//
//    public static File replaceExtension( File f, String newExtension ) {
//        return new File( f.getParent(), replaceExtension( f.getName(), newExtension ) );
//    }
//
//    public static String replaceExtension( String f, String newExtension ) {
//        String result = getBaseName( f );
//        if ( newExtension != null && newExtension.length() > 0 ) {
//            if ( newExtension.charAt( 0 ) != '.' ) {
//                result += ".";
//            }
//            result += newExtension;
//        }
//        return result;
//    }
//
//    public static String replaceBaseName( String f, String newBaseName ) {
//        return replaceBaseName( new File( f ), newBaseName ).getPath();
//    }
//
//    public static File replaceBaseName( File f, String newBaseName ) {
//        String extension = getExtension( f );
//        return new File( f.getParent(), extension.length() > 0 ? newBaseName + "." + extension : newBaseName );
//    }
//    
//    /** If the destination file exists, then rename it to a temporary name (starting with $$).
//     * If that succeeds, then rename the source file to the destination file.  If that succeeds,
//     * then delete the temporary holding file.  If the destination already exists and has the same
//     * content as the source file, then no rename is nedded and the source file is deleted. */
//    public static void twoPhaseRename( final File destFile, final File sourceFile ) throws IOException {
//        File destinationFileTempHolding = null;
//        File srcFile = sourceFile;
//        try {
//            if ( destFile.exists() ) {
//                if ( !contentEquals( destFile, srcFile ) ) { 
//                    destinationFileTempHolding = new File( destFile.getParent(), "$$" + destFile.getName() );
//                    if ( destinationFileTempHolding.exists() ) {
//                        destinationFileTempHolding.delete();
//                    }
//                    if ( !destFile.renameTo( destinationFileTempHolding ) ) {
//                        throw new IOException( "Unable to rename existing destination file: " + destFile );
//                    }
//                }
//                else {
//                    SLogging.log( "Rename skipped: " + srcFile + " => " + destFile + " because contents are equal" );
//                    srcFile.delete();
//                    srcFile = null;
//                }
//            }
//            if ( srcFile != null && !srcFile.renameTo( destFile ) ) {
//                if ( destinationFileTempHolding != null ) {
//                    destinationFileTempHolding.renameTo( destFile ); // rename it back, it just has to work
//                }
//                throw new IOException( "Unable to rename source file to destination file: " + destFile );
//            }
//        }
//        finally {
//            // just in case for some reason the temporary holding file is sitting around
//            if ( destFile.exists() && destinationFileTempHolding != null && destinationFileTempHolding.exists() ) {
//                destinationFileTempHolding.delete();
//            }
//        }
//    }
//    
//    /** Compare two files and return true if they are byte for byte the same */
//    public static boolean contentEquals( File fileA, File fileB ) {
//        if ( fileA.equals( fileB ) ) {
//            return true;
//        }
//        if ( fileA.exists() == false && fileB.exists() == false ) {
//            return true;
//        }
//        if ( fileA.exists() != fileB.exists() ) {
//            return false;
//        }
//        if ( fileA.length() != fileB.length() ) {
//            return false;
//        }
//        
//        DataInputStream inA = null;
//        DataInputStream inB = null;
//        try {
//            inA = new DataInputStream( new FileInputStream( fileA ) );
//            inB = new DataInputStream( new FileInputStream( fileB ) );
//            
//            byte[] bufferA = new byte[ 16384 ];
//            byte[] bufferB = new byte[ bufferA.length ];
//            
//            for ( long remaining = fileA.length(); remaining > 0; ) {
//                int toRead = (int) Math.min( remaining, bufferA.length );
//                inA.readFully( bufferA, 0, toRead );
//                inB.readFully( bufferB, 0, toRead );
//                
//                for ( int idx = 0; idx < toRead; ++idx ) {
//                    if ( bufferA[ idx ] != bufferB[ idx ] ) {
//                        return false;
//                    }
//                }
//                
//                remaining -= toRead;
//            }
//        }
//        catch ( Exception ioe ) {
//            SLogging.log( ioe ); // An error here should just indicate that files do not match
//            return false;
//        }
//        finally {
//            FileUtilities.closeSilently( inB ); // Exception from close is not relevant
//            FileUtilities.closeSilently( inA );
//        }
//        
//        return true;
//    }
//    
//    public static void directoryListing( PrintStream out, File dir ) {
//        if ( dir == null || out == null ) {
//            return;
//        }
//        
//        File[] files = dir.listFiles();
//        if ( files != null ) {
//            SimpleDateFormat sdfRecent = new SimpleDateFormat( "MMM dd HH:mm" );
//            SimpleDateFormat sdfNotRecent = new SimpleDateFormat( "MMM dd  yyyy" );
//            
//            for ( File f : files ) {
//                StringBuilder descriptor = new StringBuilder();
//                
//                descriptor.append( f.isDirectory() ? "d" : "-" );
//                descriptor.append( f.canRead() ? "r" : "-" );
//                descriptor.append( f.canWrite() ? "w" : "-" );
//                descriptor.append( "-" );
//                
//                boolean recent = (System.currentTimeMillis() - f.lastModified()) < 365L*24*60*60*1000;
//                
//                String s = String.format( "%s %9d %s %s", 
//                        descriptor, 
//                        f.length(), 
//                        (recent ? sdfRecent : sdfNotRecent).format( new Date( f.lastModified() ) ),
//                        f.getName()
//                );
//                out.println( s );
//            }
//        }
//    }
//    
//    /**@return true when file is accessible for Read/Write
//     * 
//     * @param filePatch patch to file
//     * @param errorBuilder storage for errors, will be NOT empty when returns false*/
//    public static boolean canReadWrite(String filePatch, StringBuilder errorBuilder) {
//        return canReadWrite(new File(filePatch), errorBuilder);
//    }
//    
//    /**@return true when file is accessible for Read/Write
//     * 
//     * @param file file to read/write
//     * @param errorBuilder storage for errors, will be NOT empty when returns false*/
//    public static boolean canReadWrite(File file, StringBuilder errorBuilder) {
//        if (!file.exists()) {
//            return true;
//        }
//        boolean canReadWrite;
//        FileLock lock = null;
//        FileChannel chanel = null;
//        try {
//            chanel = new RandomAccessFile(file, FILE_ACCESS_MODE).getChannel();
//            lock = chanel.tryLock();
//            canReadWrite = true;
//        } catch (Exception e) {
//            errorBuilder.append(e.getMessage());
//            canReadWrite = false;
//        } finally {
//            if (lock != null) {
//                try {
//                    lock.release();
//                } catch (IOException e) {
//                    SLogging.anticipatedErr("Can't release lock", e);
//                }
//            }
//            if (chanel != null) {
//                try {
//                    chanel.close();
//                } catch (IOException e) {
//                    SLogging.anticipatedErr("Can't close chanel", e);
//                }
//            }
//        }
//        return canReadWrite;
//    }
//    
//    public static byte[] readAll(InputStream inputStream) throws IOException {
//        byte[] buffer = new byte[65536];
//        ByteArrayOutputStream output = new ByteArrayOutputStream();
//        while (inputStream.read(buffer) > 0) {
//            output.write(buffer);
//        }
//        return output.toByteArray();
//    }
//    
//    /** @return URL of dir+filename if it exists, null if not */
//    public static URL toUrlIfExists(File file) {
//        try {
//            if (file.exists()) {
//                return file.toURI().toURL();
//            }
//        } catch (MalformedURLException e) {
//            SLogging.anticipatedErr(e);
//        }
//        return null;
//    }
//    
//    /* ******************************************************************************************
//     *                                    SameThreadStatus
//     *******************************************************************************************/
//    private static final class SameThreadStatus extends FileStatus {
//        @Override public void failed(String string) {/*nothing*/}
//        // public void finished(int status) {/*nothing*/} // NOTE this was not allowing the finished status to be updated
//        @Override protected void done(int status) {/*nothing*/}
//    }
}
