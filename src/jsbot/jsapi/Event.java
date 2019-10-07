package jsbot.jsapi;

import jsbot.JSBot;
import jsbot.UtilsKt;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Created on 07/10/2019.
 */
public class Event extends ScriptableObject {

    public static final String TEXT_MESSAGE_EVENT_TYPE = "msg_text";
    public static final String MEDIA_MESSAGE_EVENT_TYPE = "msg_media";

    private String type;
    private Object content;


    @Override
    public String getClassName() {
        return "Event";
    }

    @SuppressWarnings("unused")
    public static Event jsConstructor(Context cx, Object[] args, Function ctorObj, boolean inNewExpr) {
        if(inNewExpr){
            if (args.length < 2) {
                throw new JSBot.JSBotException("Missing required argument 2");
            }
            Event result = new Event();

            Object typeArg = args[0];
            if (typeArg instanceof String) {
                result.type = ((String) typeArg);
            } else {
                throw new JSBot.JSBotException("Invalid type argument type");
            }
            result.content = args[1];

            return result;
        }else{
            return null;
        }
    }

    public String jsGet_eventType(){
        return type;
    }

    public Object jsGet_content(){
        return content;
    }

    public String jsFunction_toSource(){
        String cont;
        if(content instanceof Scriptable){
            cont = UtilsKt.serialize(((Scriptable) content), null);
        }else{
            Object toJS = Context.javaToJS(content, this);
            if(toJS instanceof Scriptable) {
                cont = UtilsKt.serialize(((Scriptable) toJS), null);
            }else{
                cont = Context.toString(toJS);
            }
        }
        return "new Event('"+type+"', ("+cont+"))";
    }

    public Event init(String type, Object content){
        this.type = type;
        this.content = content;
        return this;
    }

    public Scriptable toJS(Context cx, Scriptable scope) {
        Scriptable result = cx.newObject(scope);
        ScriptableObject.putProperty(result, "eventType", type);

        Object jscontent;
        if (content instanceof Media) {
            jscontent = ((Media) content).toJS(cx, scope);
        } else {
            jscontent = content;
        }

        ScriptableObject.putProperty(result, "content", jscontent);
        return result;
    }
}
