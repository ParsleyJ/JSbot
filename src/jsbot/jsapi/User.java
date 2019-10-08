package jsbot.jsapi;

import jsbot.JSBot;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import static com.google.inject.util.Types.arrayOf;

/**
 * Java bean with special methods to be used in Rhino engine.
 */
public class User extends ScriptableObject {

    private Integer id;
    private String username = null;

    public User() {
    }


    @Override
    public String getClassName() {
        return "User";
    }


    @SuppressWarnings("unused")
    public static User jsConstructor(Context cx, Object[] args, Function ctorObj, boolean inNewExpr) {
        if (inNewExpr) {

            if (args.length == 0) {
                throw new JSBot.JSBotException("Missing required id argument");
            }
            User result = new User();

            Object idArg = args[0];
            if (idArg instanceof Number) {
                Number idNum = ((Number) idArg);
                result.id = idNum.intValue();
            } else {
                throw new JSBot.JSBotException("Invalid id argument type");
            }


            if (args.length > 1) {
                Object unArg = args[1];

                if (unArg instanceof String) {
                    result.username = ((String) unArg);
                } else if (unArg != null) {
                    throw new JSBot.JSBotException("Invalid username argument type");
                }

            }
            return result;
        }
        return null;
    }

    @SuppressWarnings("unused")
    public String jsFunction_toSource() {
        return "new User(" + id + ", '" + username + "')";
    }

    @SuppressWarnings("unused")
    public Integer jsGet_id() {
        return id;
    }

    @SuppressWarnings("unused")
    public String jsGet_username() {
        return username;
    }

    public Scriptable toJS(Context cx, Scriptable scope) {
        return cx.newObject(scope, "User", new Object[]{id, username});
    }

    public static User fromTgUser(org.telegram.telegrambots.meta.api.objects.User tgUser) {
        if (tgUser == null) return null;
        User result = new User();
        result.id = tgUser.getId();
        result.username = tgUser.getUserName();
        return result;
    }

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public User init(Integer id, String username) {
        this.id = id;
        this.username = username;
        return this;
    }


    public static User fromJS(Scriptable obj) {
        if(obj instanceof User){
            return ((User) obj);
        }

        Object id = obj.get("id", obj);
        Object un = obj.get("username", obj);
        if (id instanceof Integer) {
            return new User().init(((Integer) id), Context.toString(un));
        } else {
            return null;
        }
    }



}
