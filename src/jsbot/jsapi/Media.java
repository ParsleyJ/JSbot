package jsbot.jsapi;

import jsbot.JSBot;
import jsbot.JSBotException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;

/**
 * Created on 07/10/2019.
 */
public class Media extends ScriptableObject {

    public final static int FILE_LIMIT = 500000;
    public final static String ANIMATION = "ANIMATION";
    public final static String AUDIO = "AUDIO";
    public final static String DOCUMENT = "DOCUMENT";
    public final static String PHOTO = "PHOTO";
    public final static String STICKER = "STICKER";
    public final static String VIDEO = "VIDEO";
    public final static String VIDEO_NOTE = "VIDEO_NOTE";
    public final static String VOICE = "VOICE";


    private String mediaType;
    private String fileID;

    @Override
    public String getClassName() {
        return "Media";
    }


    @SuppressWarnings("unused")
    public static Media jsConstructor(Context cx, Object[] args, Function ctorObj, boolean inNewExpr) {
        if (inNewExpr) {
            if (args.length < 2) {
                throw new JSBotException("Missing required argument 2");
            }
            Media result = new Media();

            Object typeArg = args[0];
            if (typeArg instanceof String) {
                result.mediaType = ((String) typeArg);
            } else {
                throw new JSBotException("Invalid mediaType argument type");
            }

            Object idArg = args[1];
            if (idArg instanceof String) {
                result.fileID = ((String) idArg);
            } else {
                throw new JSBotException("Invalid fileID argument type");
            }

            return result;
        } else {
            return null;
        }
    }


    @SuppressWarnings("unused")
    public String jsGet_mediaType() {
        return mediaType;
    }

    @SuppressWarnings("unused")
    public String jsGet_fileID() {
        return fileID;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getFileID() {
        return fileID;
    }

    public String jsFunction_toSource() {
        return "new Media('" + mediaType + "', '" + fileID + "')";
    }

    public String getDocumentContents(JSBot bot) {
        if (mediaType.equals(DOCUMENT)) {
            File tgFile;
            try {
                tgFile = bot.execute(new GetFile().setFileId(fileID));


                int fileSize = tgFile.getFileSize();
                if (fileSize > FILE_LIMIT) {
                    throw new JSBotException("File too big: $fileSize");
                }
                InputStream stream = bot.downloadFileAsStream(tgFile);

                StringWriter sw = new StringWriter();
                BufferedReader buffReader = new BufferedReader(new InputStreamReader(stream));
                char[] buffer = new char[8 * 1024];
                var chars = buffReader.read(buffer);
                while (chars >= 0) {
                    sw.write(buffer, 0, chars);
                    chars = buffReader.read(buffer);
                }
                return sw.toString();
            } catch (TelegramApiException | IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            throw new JSBotException("Invalid media type");
        }
    }

    public Media init(String mediaType, String fileID) {
        this.mediaType = mediaType;
        this.fileID = fileID;
        return this;
    }

    public Scriptable toJS(Context cx, Scriptable scope) {
        return cx.newObject(scope, "Media", new Object[]{mediaType, fileID});
    }

    public static Media fromJS(Scriptable obj){
        if(obj instanceof Media){
            return ((Media) obj);
        }

        Object mediaType = obj.get("mediaType", obj);
        Object fileID = obj.get("fileID", obj);
        if (mediaType instanceof String && fileID instanceof String) {
            return new Media().init(((String) mediaType), ((String) fileID));
        } else {
            return null;
        }
    }
}
