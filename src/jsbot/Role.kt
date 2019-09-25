package jsbot

/**
 * Created on 23/09/2019.
 *
 */
interface Role {

    fun getRoleName(): String

    fun getAbilites(): Set<String>

    fun isAuthorized(ability: String) = getAbilites().contains(ability)

    companion object {

        const val NOT_AUTHORIZED_ROLE = "NOT_AUTHORIZED"
        const val GROUP_USER_ROLE = "GROUP_USER"
        const val USER_ROLE = "USER"
        const val MODERATOR_ROLE = "MODERATOR"
        const val ADMIN_ROLE = "ADMIN"
        const val SUPER_ADMIN_ROLE = "SUPER_ADMIN"


        const val PRIVATE_USE_BOT_ABILITY = "PRIVATE_USE_BOT_ABILITY"
        const val GROUP_USE_BOT_ABILITY = "GROUP_USE_BOT_ABILITY"
        const val PANIC_ABILITY = "PANIC_ABILITY"
        const val SET_ROLES_ABILITY = "SET_ROLES_ABILITY"
        const val JAVA_ABILITY = "JAVA_ABILITY"
        const val BOT_ACCESS_ABILITY = "BOT_ACCESS_ABILITY"
        const val LOAD_FILE_ABILITY = "LOAD_FILE_ABILITY"
        const val USER_DATABASE_READ_ABILITY = "USER_DATABASE_READ_ABILITY"
        const val PRIVATE_MESSAGING_ABILITY = "PRIVATE_MESSAGING_ABILITY"

        fun create(roletype: String) = when (roletype) {
            NOT_AUTHORIZED_ROLE -> NotAuthorized()
            GROUP_USER_ROLE -> GroupUser()
            USER_ROLE -> User()
            MODERATOR_ROLE -> Moderator()
            ADMIN_ROLE -> Admin()
            SUPER_ADMIN_ROLE -> SuperAdmin()
            else -> null
        }

        fun genRole(roletype: String, default: String): Role {
            return create(roletype) ?: create(default)!!
        }

        open class NotAuthorized : Role {
            override fun getRoleName() = NOT_AUTHORIZED_ROLE
            override fun getAbilites() = setOf<String>()
        }

        open class GroupUser : Role {
            override fun getRoleName() = GROUP_USER_ROLE
            override fun getAbilites() = setOf(
                GROUP_USE_BOT_ABILITY
            )
        }

        open class User : GroupUser() {
            override fun getRoleName() = USER_ROLE
            override fun getAbilites() = super.getAbilites() + setOf(
                PRIVATE_USE_BOT_ABILITY
            )
        }

        open class Moderator : User() {
            override fun getRoleName() = MODERATOR_ROLE
            override fun getAbilites() = super.getAbilites() + setOf(
                PANIC_ABILITY,
                USER_DATABASE_READ_ABILITY,
                PRIVATE_MESSAGING_ABILITY
            )
        }

        open class Admin : Moderator() {
            override fun getRoleName() = ADMIN_ROLE
            override fun getAbilites() = super.getAbilites() + setOf(
                SET_ROLES_ABILITY
            )
        }

        open class SuperAdmin : Admin() {
            override fun getRoleName() = SUPER_ADMIN_ROLE
            override fun getAbilites() = super.getAbilites() + setOf(
                JAVA_ABILITY,
                BOT_ACCESS_ABILITY,
                LOAD_FILE_ABILITY
            )
        }

    }

}