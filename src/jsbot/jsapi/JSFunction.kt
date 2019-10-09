package jsbot.jsapi

import jsbot.JSBotException
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

/**
 * Created on 28/09/2019.
 *
 */
open class JSFunction(
    private val metaArity: Int,
    private val func: (Context, Scriptable, Scriptable, Array<out Any>) -> Any?
) : BaseFunction() {
    override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any>): Any? {
        return func.invoke(cx, scope, thisObj, args)
    }

    override fun getArity(): Int {
        return metaArity
    }


}

inline fun <reified T1> jsFun(crossinline func: (Context, Scriptable, T1) -> Any?): JSFunction {
    return JSFunction(1) { cx, scope, _, args ->
        if (args.isEmpty()) {
            throw JSBotException("Missing argument")
        }
        val arg = args[0]
        if (arg !is T1) {
            throw JSBotException("Invalid argument type")
        }
        return@JSFunction func.invoke(cx, scope, arg)
    }
}

inline fun <reified T1> jsFun(crossinline func: (T1) -> Any?): JSFunction {
    return JSFunction(1) { _, _, _, args ->
        if (args.isEmpty()) {
            throw JSBotException("Missing argument")
        }
        val arg = args[0]
        if (arg !is T1) {
            throw JSBotException("Invalid argument type")
        }
        return@JSFunction func.invoke(arg)
    }
}

inline fun <reified T1, reified T2> jsFun(crossinline func: (T1, T2) -> Any?): JSFunction {
    return JSFunction(2) { _, _, _, args ->
        if (args.size < 2) {
            throw JSBotException("Missing arguments")
        }
        val arg0 = args[0]
        val arg1 = args[1]
        if (arg0 !is T1) {
            throw JSBotException("Invalid argument 1 type")
        }
        if (arg1 !is T2) {
            throw JSBotException("Invalid argument 2 type")
        }
        return@JSFunction func.invoke(arg0, arg1)
    }
}

inline fun <reified T1, reified T2> jsFun(crossinline ifOneArg: (T1) -> Any?, crossinline ifTwoArgs: (T1, T2) -> Any?): JSFunction {
    return JSFunction(2) { _, _, _, args ->
        if (args.isEmpty()) {
            throw JSBotException("Missing arguments")
        }
        val arg0 = args[0]
        if (arg0 !is T1) {
            throw JSBotException("Invalid argument 1 type")
        }
        if(args.size == 1){
            return@JSFunction ifOneArg.invoke(arg0)
        }
        val arg1 = args[1]
        if (arg1 !is T2) {
            throw JSBotException("Invalid argument 2 type")
        }
        return@JSFunction ifTwoArgs.invoke(arg0, arg1)
    }
}

inline fun jsFun(crossinline func: () -> Any?) = JSFunction(0) { _, _, _, _ -> func.invoke() }

inline fun <reified O> jsMethod(crossinline func: (Context, Scriptable, O) -> Any?): JSFunction {
    return JSFunction(0) { cx, scope, thisObj, _ ->
        if (thisObj !is O) {
            throw JSBotException("Invalid argument type")
        }
        return@JSFunction func.invoke(cx, scope, thisObj)
    }
}

inline fun <reified O> jsMethod(crossinline func: (O) -> Any?): JSFunction {
    return JSFunction(0) { _, _, thisObj, _ ->
        if (thisObj !is O) {
            throw JSBotException("Invalid argument type")
        }
        return@JSFunction func.invoke(thisObj)
    }
}

inline fun <reified O, reified T> jsMethod(crossinline func: (O, T) -> Any?): JSFunction {
    return JSFunction(0) { _, _, thisObj, args ->
        if (args.isEmpty()) {
            throw JSBotException("Missing arguments")
        }
        if (thisObj !is O) {
            throw JSBotException("Invalid argument type")
        }
        val arg0 = args[0]
        if (arg0 !is T) {
            throw JSBotException("Invalid argument 1 type")
        }
        return@JSFunction func.invoke(thisObj, arg0)
    }
}