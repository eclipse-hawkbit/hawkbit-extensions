# hawkBit multi user extension

This extension allows you to statically configure multiple users through the application properties.
When the extension is enabled but no users are configured, it will fall back to the standard security.user configuration.

## Example configuration

A list of permissions with description is [available from the hawkBit repository](https://github.com/eclipse/hawkbit/blob/master/hawkbit-security-core/src/main/java/org/eclipse/hawkbit/im/authentication/SpPermission.java).
Additionally, if a single permission with the name *ALL* is configured, that user will be granted all available permissions.

    hawkbit.server.im.users[0].username=admin
    hawkbit.server.im.users[0].password={noop}admin
    hawkbit.server.im.users[0].firstname=Test
    hawkbit.server.im.users[0].lastname=Admin
    hawkbit.server.im.users[0].email=admin@test.de
    hawkbit.server.im.users[0].permissions=ALL
    
    hawkbit.server.im.users[1].username=test
    hawkbit.server.im.users[1].password={noop}test
    hawkbit.server.im.users[1].firstname=Test
    hawkbit.server.im.users[1].lastname=Tester
    hawkbit.server.im.users[1].email=test@tester.com
    hawkbit.server.im.users[1].permissions=READ_TARGET,UPDATE_TARGET,CREATE_TARGET,DELETE_TARGET

The password encoder used is specified in brackets. In this example, *noop* is used as the plaintext encoder.
For production use, it is recommended to use a hash function designed for passwords such as *bcrypt*. See this [blog post](https://spring.io/blog/2017/11/01/spring-security-5-0-0-rc1-released#password-storage-format) for more information on password encoders in Spring Security 5.
