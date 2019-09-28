package jsbot

import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

/**
 * Created on 28/09/2019.
 *
 */
class JSFunction(
    private val metaArity:Int,
    private val func: (Context, Scriptable, Scriptable, Array<out Any>)-> Any?
) : BaseFunction() {
    override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any>): Any? {
        return func.invoke(cx, scope, thisObj, args)
    }

    override fun getArity(): Int {
        return metaArity
    }
}