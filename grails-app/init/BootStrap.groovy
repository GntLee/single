import com.system.Role
import com.system.User
import com.system.UserRole

class BootStrap {

    def init = { servletContext ->

        //创建角色
        def role1 = new Role(authority: "ROLE_ADMIN").save()
        def role2 = new Role(authority: "ROLE_SUPSYS").save()
        def role3 = new Role(authority: "ROLE_USER").save()

        //创建用户
        def user1 = new User(username: "admin", password: "admin").save()
        def user2 = new User(username: "super", password: "super").save()
        def user3 = new User(username: "user", password: "user").save()

        //用户角色关联
        UserRole.create user1, role1, true
        UserRole.create user2, role2, true
        UserRole.create user3, role3, true
    }
    def destroy = {

    }
}
