package jsbot

import org.json.JSONObject
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

/**
 * Created on 28/09/2019.
 *
 */
fun List<String>.toScriptable(cx: Context, scope: Scriptable): Any {
    return cx.newArray(scope, this.map { Context.javaToJS(it, scope) }.toTypedArray())
}

fun JSONObject.toScriptable(cx: Context, scope: Scriptable): Any {
    return cx.evaluateString(scope, "(${this})", "toScriptable", 1, null)
}

fun String.toScriptable(cx: Context, scope: Scriptable): Any {
    return cx.evaluateString(scope, "\"${this}\"", "toScriptable", 1, null)
}



fun Scriptable.serialize(context: Context): String {
    val result = context.evaluateString(
        this,
        "toSource()",
        "<save>",
        1,
        null
    )
    return Context.toString(result)
}
