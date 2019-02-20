#开发工具

    Intellij IDEA 2017.2
#开发环境：

    | Grails Version: 3.1.5
    | Groovy Version: 2.4.6
    | JVM Version: 1.8.0_144

#本项目为grails+spring security实现单端登录
    注意：此处当用户并非单点登录，主要实现功能效果防止多端登录，一方登录，另一方强制下线。
#创建项目（略）
项目创建完成后，在build.gradle中引入spring security依赖：
    
    buildscript {
        ext {
            grailsVersion = project.grailsVersion
        }
        repositories {
            mavenLocal()
            maven { url "https://repo.grails.org/grails/core" }
        }
        dependencies {
            classpath "org.grails:grails-gradle-plugin:$grailsVersion"
            classpath "com.bertramlabs.plugins:asset-pipeline-gradle:2.8.2"
            classpath "org.grails.plugins:hibernate4:5.0.5"
        }
    }
    
    version "0.1"
    group "springsecurity"
    
    apply plugin:"eclipse"
    apply plugin:"idea"
    apply plugin:"war"
    apply plugin:"org.grails.grails-web"
    apply plugin:"org.grails.grails-gsp"
    apply plugin:"asset-pipeline"
    
    ext {
        grailsVersion = project.grailsVersion
        gradleWrapperVersion = project.gradleWrapperVersion
    }
    
    repositories {
        mavenLocal()
        maven { url "https://repo.grails.org/grails/core" }
    }
    
    dependencyManagement {
        imports {
            mavenBom "org.grails:grails-bom:$grailsVersion"
        }
        applyMavenExclusions false
    }
    
    dependencies {
    
        //spring security权限依赖
        compile 'org.grails.plugins:spring-security-core:3.1.2'
    
        compile "org.springframework.boot:spring-boot-starter-logging"
        compile "org.springframework.boot:spring-boot-autoconfigure"
        compile "org.grails:grails-core"
        compile "org.springframework.boot:spring-boot-starter-actuator"
        compile "org.springframework.boot:spring-boot-starter-tomcat"
        compile "org.grails:grails-dependencies"
        compile "org.grails:grails-web-boot"
        compile "org.grails.plugins:cache"
        compile "org.grails.plugins:scaffolding"
        compile "org.grails.plugins:hibernate4"
        compile "org.hibernate:hibernate-ehcache"
        console "org.grails:grails-console"
        profile "org.grails.profiles:web:3.1.5"
        runtime "com.bertramlabs.plugins:asset-pipeline-grails:2.8.2"
        runtime "com.h2database:h2"
        testCompile "org.grails:grails-plugin-testing"
        testCompile "org.grails.plugins:geb"
        testRuntime "org.seleniumhq.selenium:selenium-htmlunit-driver:2.47.1"
        testRuntime "net.sourceforge.htmlunit:htmlunit:2.18"
    }
    
    task wrapper(type: Wrapper) {
        gradleVersion = gradleWrapperVersion
    }
    
    assets {
        minifyJs = true
        minifyCss = true
    }

依赖下载完成后，按住Ctrl+Alt+G，在commond里面输入：

    s2-quickstart com.system User Role
    
他会自动帮你创建User、Role以及UserRole类，grails-app/conf/application.groovy也会自动配置好，执行完成控制台提示:

    |Creating User class 'User' and Role class 'Role' in package 'com.system'
    |Rendered template Person.groovy.template to destination grails-app\domain\com\system\User.groovy
    |Rendered template Authority.groovy.template to destination grails-app\domain\com\system\Role.groovy
    |Rendered template PersonAuthority.groovy.template to destination grails-app\domain\com\system\UserRole.groovy
    |
    ************************************************************
    * Created security-related domain classes. Your            *
    * grails-app/conf/application.groovy has been updated with *
    * the class names of the configured domain classes;        *
    * please verify that the values are correct.               *
    ************************************************************

在src/main/groovy下面创建package为com.custom并创建ConcurrentSingleSessionAuthenticationStrategy类

    package com.custom
    
    import org.springframework.security.core.Authentication
    import org.springframework.security.core.session.SessionRegistry
    import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
    import org.springframework.util.Assert
    
    import javax.servlet.http.HttpServletRequest
    import javax.servlet.http.HttpServletResponse
    /**
     * 会话管理类
     * @auther Lee
     * @Date 2017/11/14 18:21
     * return
     */
    class ConcurrentSingleSessionAuthenticationStrategy implements SessionAuthenticationStrategy {
    
        private SessionRegistry sessionRegistry
    
        /**
         * @param 将新的会话赋值给sessionRegistry
         */
        public ConcurrentSingleSessionAuthenticationStrategy(SessionRegistry sessionRegistry) {
            Assert.notNull(sessionRegistry, "SessionRegistry cannot be null")
            this.sessionRegistry = sessionRegistry
        }
        /**
         * 覆盖父类的onAuthentication方法
         * 用新的session替换就的session
         */
        public void onAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
    
            def sessions = sessionRegistry.getAllSessions(authentication.getPrincipal(), false)
            def principals = sessionRegistry.getAllPrincipals()
            sessions.each {
                if (it.principal == authentication.getPrincipal()) {
                    it.expireNow()
                }
            }
    
    
        }
    }
    
打开grails-app/conf/spring/resource.groovy，配置DSL,注意导包
    
    import com.custom.ConcurrentSingleSessionAuthenticationStrategy
    import org.springframework.security.core.session.SessionRegistryImpl
    import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy
    import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy
    import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy
    import org.springframework.security.web.session.ConcurrentSessionFilter
    
    // Place your Spring DSL code here
    beans = {
    
        sessionRegistry(SessionRegistryImpl)
        //很重要
        sessionFixationProtectionStrategy(SessionFixationProtectionStrategy){
            migrateSessionAttributes = true
            alwaysCreateSession = true
        }
        // "/login/already"为重定向请求
        concurrentSingleSessionAuthenticationStrategy(ConcurrentSingleSessionAuthenticationStrategy,ref('sessionRegistry'))
        registerSessionAuthenticationStrategy(RegisterSessionAuthenticationStrategy,ref('sessionRegistry'))
        sessionAuthenticationStrategy(CompositeSessionAuthenticationStrategy,[ref('concurrentSingleSessionAuthenticationStrategy'), ref('sessionFixationProtectionStrategy'), ref('registerSessionAuthenticationStrategy')])
        concurrentSessionFilter(ConcurrentSessionFilter, ref('sessionRegistry'), "/login/already")
        
        // grails3.3.0用下面这个，查看源码发现ConcurrentSessionFilter类中重定向url的构造方法已被废弃，不生效
        //concurrentSessionFilter(ConcurrentSessionFilter, ref('sessionRegistry'))
    }
    
打开grails-app/conf/application.groovy，最后面加入

    grails.plugin.springsecurity.filterChain.filterNames = [ 'securityContextPersistenceFilter', 'logoutFilter', 'concurrentSessionFilter', 'rememberMeAuthenticationFilter', 'anonymousAuthenticationFilter', 'exceptionTranslationFilter', 'filterInvocationInterceptor' ]
    
在init/BootStrap.groovy下面的init方法中添加启动任务，注意添加构造方法

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
    
启动项目，访问http://localhost:8080

常见问题：

    启动报错====>配置问题
    
    无法登陆====>有可能用户密码未加密，请加密后再次运行
    
    没有LoginController和auth.gsp怎么办？
    1、找到spring-security-core包
    2、拷贝login到你项目的views下面，可以自行修改样式
    3、拷贝grails/plugin/springsecurity/LoginController到你项目的controller下面，自己建包
    
