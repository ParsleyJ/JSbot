package jsbot.jsapi

/**
 * Created on 23/09/2019.
 *
 */
interface Role {

    fun getRoleName(): String

    fun getAbilites(): Set<String>

    fun getValue(): Int

    fun isAuthorized(ability: String) = getAbilites().contains(ability)

    fun isChangeRoleAuthorized(targetUser: Role, toRole: Role): Boolean {
        return isAuthorized(SET_ROLES_ABILITY)
                && targetUser.getValue() < this.getValue()
                && toRole.getValue() <= this.getValue()
    }

    companion object {

        const val NOT_AUTHORIZED_ROLE = "NOT_AUTHORIZED"
        const val GROUP_USER_ROLE = "GROUP_USER"
        const val USER_ROLE = "USER"
        const val MODERATOR_ROLE = "MODERATOR"
        const val ADMIN_ROLE = "ADMIN"
        const val SUPER_ADMIN_ROLE = "SUPER_ADMIN"

        const val PRIVATE_USE_BOT_ABILITY = "PRIVATE_USE_BOT"
        const val GROUP_USE_BOT_ABILITY = "GROUP_USE_BOT"
        const val PANIC_ABILITY = "PANIC"
        const val SET_ROLES_ABILITY = "SET_ROLES"
        const val JAVA_ABILITY = "JAVA"
        const val BOT_ACCESS_ABILITY = "BOT_ACCESS"
        const val LOAD_FILE_ABILITY = "LOAD_FILE"
        const val USER_DATABASE_READ_ABILITY = "USER_DATABASE_READ"
        const val PRIVATE_MESSAGING_ABILITY = "PRIVATE_MESSAGING"
        const val CHANGE_HANDLERS_ABILITY = "CHANGE_HANDLERS"
        const val TAMPER_MESSAGES_ABILITY = "TAMPER_MESSAGES"
        const val DISGUISE_ABILITY = "DISGUISE"


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
            override fun getValue() = 0
        }

        open class GroupUser : Role {
            override fun getRoleName() = GROUP_USER_ROLE
            override fun getAbilites() = setOf(
                GROUP_USE_BOT_ABILITY
            )

            override fun getValue() = 1
        }

        open class User : GroupUser() {
            override fun getRoleName() = USER_ROLE
            override fun getAbilites() = super.getAbilites() + setOf(
                PRIVATE_USE_BOT_ABILITY
            )

            override fun getValue() = super.getValue() + 1
        }

        open class Moderator : User() {
            override fun getRoleName() = MODERATOR_ROLE
            override fun getAbilites() = super.getAbilites() + setOf(
                PANIC_ABILITY,
                USER_DATABASE_READ_ABILITY,
                PRIVATE_MESSAGING_ABILITY,
                SET_ROLES_ABILITY
            )

            override fun getValue() = super.getValue() + 1
        }

        open class Admin : Moderator() {
            override fun getRoleName() = ADMIN_ROLE
            override fun getAbilites() = super.getAbilites() + setOf(
                CHANGE_HANDLERS_ABILITY,
                TAMPER_MESSAGES_ABILITY
            )

            override fun getValue() = super.getValue() + 1
        }

        open class SuperAdmin : Admin() {
            override fun getRoleName() = SUPER_ADMIN_ROLE
            override fun getAbilites() = super.getAbilites() + setOf(
                JAVA_ABILITY,
                BOT_ACCESS_ABILITY,
                LOAD_FILE_ABILITY,
                DISGUISE_ABILITY
            )

            override fun getValue() = super.getValue() + 1
        }

    }

}