<img src=hawkbit_logo.png width=533 height=246 />

# Eclipse hawkBit™ - Extensions collection

Build: [![Circle CI](https://circleci.com/gh/eclipse/hawkbit-extensions.svg?style=shield)](https://circleci.com/gh/eclipse/hawkbit-extensions)

[hawkBit](https://github.com/eclipse/hawkbit) extensions are implementations to extend the functionality of hawkBit which are maintained by the hawkBit community. The extensions can be used to integrate in a hawkBit application to exchange or extend hawkBit functionality. Extensions should work with the standard [hawkBit runtime](https://github.com/eclipse/hawkbit/tree/master/hawkbit-runtime/hawkbit-update-server). All extensions provide a `README.md` which explains the use of the extension and how to use them.

hawkBit extensions are implementations which are not included in the default implementation of hawkBit's security and auto-configuration mechanism or extending functionality by e.g. integrating third-party services to hawkBit. 

hawkBit makes use of the spring-bean and configuration mechanism which allows an flexible configuration and the exchange of beans in spring-configurations. Many beans are `@Conditional` annotated in hawkBit so they can be overwritten. Extensions can also leverage and implemented functionalities based on hawkBit's event mechanism by subscribing to events and implement additional functionality. 

## hawkBit extension guidelines

* Containing a `README.md` which explains the extension in detail and how to use it
* Working with the hawkBit example application
* Following the maven-artifact-id `hawkbit-extension-<name>`
