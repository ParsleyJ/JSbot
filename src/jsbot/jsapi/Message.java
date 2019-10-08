package jsbot.jsapi;

import jsbot.JSBot;
import jsbot.UtilsKt;
import org.mozilla.javascript.*;

/**
 * Created on 07/10/2019.
 */
public class Message extends ScriptableObject {

    private Integer messageID = null;
    private Long chatID = null;
    private Media mediaContent = null;
    private String textContent = null;
    private User from = null;

    public static Message jsConstructor(Context cx, Object[] args, Function ctorObj, boolean inNewExpr) {
        Message result = new Message();
        if (args.length > 0) {
            Object arg = args[0];
            if (arg instanceof Scriptable) {
                Scriptable scriptableArg = (Scriptable) arg;
                Object messageID = scriptableArg.get("messageID", scriptableArg);
                if (messageID != null && !messageID.equals(Scriptable.NOT_FOUND) && !Undefined.isUndefined(messageID)) {
                    if (messageID instanceof Number) {
                        result.messageID = ((Number) messageID).intValue();
                    } else if (messageID instanceof String) {
                        try {
                            result.messageID = Integer.parseInt(((String) messageID));
                        } catch (NumberFormatException ignored) {
                            throw new JSBot.JSBotException("Invalid message ID");
                        }
                    } else {
                        throw new JSBot.JSBotException("Invalid messageID argument type");
                    }
                }
                Object chatID = scriptableArg.get("chatID", scriptableArg);
                if (chatID != null && !chatID.equals(Scriptable.NOT_FOUND) && !Undefined.isUndefined(chatID)) {
                    if (chatID instanceof Number) {
                        result.chatID = ((Number) chatID).longValue();
                    } else if (chatID instanceof String) {
                        try {
                            result.chatID = Long.parseLong(((String) chatID));
                        } catch (NumberFormatException ignored) {
                            throw new JSBot.JSBotException("Invalid chat ID");
                        }
                    } else {
                        throw new JSBot.JSBotException("Invalid chatID argument type");
                    }
                }
                Object mediaContent = scriptableArg.get("mediaContent", scriptableArg);
                if (mediaContent != null && !mediaContent.equals(Scriptable.NOT_FOUND) && !Undefined.isUndefined(mediaContent)) {
                    if (mediaContent instanceof Scriptable) {
                        result.mediaContent = Media.fromJS((Scriptable) mediaContent);
                    } else {
                        throw new JSBot.JSBotException("Invalid mediaContent argument type");
                    }
                }
                Object textContent = scriptableArg.get("textContent", scriptableArg);
                if (textContent != null && !textContent.equals(Scriptable.NOT_FOUND) && !Undefined.isUndefined(textContent)) {
                    if (textContent instanceof String) {
                        result.textContent = ((String) textContent);
                    } else {
                        result.textContent = Context.toString(textContent);
                    }
                }
                Object from = scriptableArg.get("from", scriptableArg);
                if (from != null && !from.equals(Scriptable.NOT_FOUND) && !Undefined.isUndefined(from)) {
                    if (from instanceof Scriptable) {
                        result.from = User.fromJS((Scriptable) from);
                    } else {
                        throw new JSBot.JSBotException("Invalid from argument type");
                    }
                }
            } else {
                throw new JSBot.JSBotException("Unexpected message initializer object");
            }
        }
        return result;
    }


    public String jsFunction_toSource() {
        Context cx = Context.getCurrentContext();
        Scriptable arg = getInitializerObject(cx);
        String argStr = UtilsKt.serialize(arg, cx);
        return "new Message(" + argStr + ")";
    }

    private Scriptable getInitializerObject(Context cx) {
        Scriptable arg = cx.newObject(this);
        ScriptableObject.putProperty(arg, "messageID", messageID);
        ScriptableObject.putProperty(arg, "chatID", chatID);
        ScriptableObject.putProperty(arg, "mediaContent", mediaContent);
        ScriptableObject.putProperty(arg, "textContent", textContent);
        ScriptableObject.putProperty(arg, "from", from);
        return arg;
    }

    public Integer jsGet_messageID() {
        return messageID;
    }

    public Long jsGet_chatID() {
        return chatID;
    }

    public Object jsGet_mediaContent() {
        return UtilsKt.toJS(mediaContent, Context.getCurrentContext(), this);
    }

    public String jsGet_textContent() {
        return textContent;
    }

    public Object jsGet_from() {
        return UtilsKt.toJS(from, Context.getCurrentContext(), this);
    }


    public Scriptable jsFunction_withMessageID(Integer arg) {
        Message result = new Message();
        result.messageID = arg;
        result.chatID = chatID;
        result.mediaContent = mediaContent;
        result.textContent = textContent;
        result.from = from;
        return result.toJS(Context.getCurrentContext(), this);
    }


    public Scriptable jsFunction_withChatID(Integer arg) {
        Message result = new Message();
        result.messageID = messageID;
        result.chatID = arg.longValue();
        result.mediaContent = mediaContent;
        result.textContent = textContent;
        result.from = from;
        return result.toJS(Context.getCurrentContext(), this);
    }


    public Scriptable jsFunction_withMediaContent(Media arg) {
        Message result = new Message();
        result.messageID = messageID;
        result.chatID = chatID;
        result.mediaContent = arg;
        result.textContent = textContent;
        result.from = from;
        return result.toJS(Context.getCurrentContext(), this);
    }


    public Scriptable jsFunction_withTextContent(String arg) {
        Message result = new Message();
        result.messageID = messageID;
        result.chatID = chatID;
        result.mediaContent = mediaContent;
        result.textContent = arg;
        result.from = from;
        return result.toJS(Context.getCurrentContext(), this);
    }


    public Scriptable jsFunction_withFrom(User arg) {
        Message result = new Message();
        result.messageID = messageID;
        result.chatID = chatID;
        result.mediaContent = mediaContent;
        result.textContent = textContent;
        result.from = arg;
        return result.toJS(Context.getCurrentContext(), this);
    }


    public Integer getMessageID() {
        return messageID;
    }

    public Long getChatID() {
        return chatID;
    }

    public Media getMediaContent() {
        return mediaContent;
    }

    public String getTextContent() {
        return textContent;
    }

    public User getFrom() {
        return from;
    }

    public Message setMessageID(Number messageID) {
        this.messageID = messageID.intValue();
        return this;
    }

    public Message setChatID(Number chatID) {
        this.chatID = chatID.longValue();
        return this;
    }

    public Message setMediaContent(Media mediaContent) {
        this.mediaContent = mediaContent;
        return this;
    }

    public Message setTextContent(String textContent) {
        this.textContent = textContent;
        return this;
    }

    public Message setFrom(User from) {
        this.from = from;
        return this;
    }

    public static Message fromTgMessage(org.telegram.telegrambots.meta.api.objects.Message message) {
        Message result = new Message();
        result.messageID = message.getMessageId();
        result.chatID = message.getChatId();
        if (UtilsKt.hasMedia(message)) {
            result.mediaContent = JSApiKt.toMedia(message);
            result.textContent = message.getCaption();
        } else {
            result.textContent = message.getText();
        }
        result.from = User.fromTgUser(message.getFrom());
        return result;
    }

    public Scriptable toJS(Context cx, Scriptable scope) {
        return cx.newObject(scope, "Message", new Object[]{getInitializerObject(cx)});
    }


    @Override
    public String getClassName() {
        return "Message";
    }
}
