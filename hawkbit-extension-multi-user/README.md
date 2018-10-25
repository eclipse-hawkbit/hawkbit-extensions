# hawkBit multi user extension

This extension allows you to statically configure multiple users through the application properties.
When the extension is enabled but no users are configured, it will fall back to the standard security.user configuration.

## Example configuration

A list of permissions with description is [available from the hawkBit repository](https://github.com/eclipse/hawkbit/blob/master/hawkbit-security-core/src/main/java/org/eclipse/hawkbit/im/authentication/SpPermission.java).
Additionally, if a single permission with the name *ALL* is configured, that user will be granted all available permissions.

    multiuser.user[0].username=admin
    multiuser.user[0].password=admin
    multiuser.user[0].firstname=Test
    multiuser.user[0].lastname=Admin
    multiuser.user[0].email=admin@test.de
    multiuser.user[0].permissions[0]=ALL
    multiuser.user[1].username=test
    multiuser.user[1].password=test
    multiuser.user[1].firstname=Test
    multiuser.user[1].lastname=Tester
    multiuser.user[1].email=test@tester.com
    multiuser.user[1].permissions[0]=READ_TARGET
    multiuser.user[1].permissions[1]=UPDATE_TARGET
    multiuser.user[1].permissions[2]=CREATE_TARGET
    multiuser.user[1].permissions[3]=DELETE_TARGET
