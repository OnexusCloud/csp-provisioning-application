Respect Network CSP Reference Implementation
============================

### Dependencies

XDI2-Client: https://github.com/projectdanube/xdi2/tree/master/client (Version 0.2-SNAPSHOT) 
SDK-CSP: https://github.com/RespectNetwork/sdk-csp/tree/release-0.1.x (Version 0.1.2) 

### How to  Build
To  build just run

    mvn clean install

### How to run

    mvn jetty:run

Then the Reference implementation is available at

        http://localhost:7073/csp-provisioning-application

First  Create an Invitation using the default CSP at

        http://localhost:7072/csp-provisioning-application/createInvitation/testCSP

This will allow you to  start the registration process

### Configuration Management.

Various configurable parameters for this service are located in property files in
./src/main/resources

To  customize your CSP's configuration use the following config. mamnagement pattern.

1) By default csp.default.properties is applied.
2) Setting the -Dregistration.env System Property e.g. -Dregistration.env=dev will use the registration.${registration.env}.properties. Valid entries are dev, stage or ote.
3) You can also  specify  a property file outside of the WAR using -Dcspprop.location=path_to_prop_file. The csp.properties file in this directory will be used.

    <bean class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer">
        <property name="locations">
            <list>
                <value>classpath:csp.default.properties</value>
                <value>classpath:csp.${csp.env}.properties</value>
                <value>file:${cspprop.location}/csp.properties</value>
            </list>
        </property>
        <property name="ignoreResourceNotFound" value="true"/>
        <property name="ignoreUnresolvablePlaceholders" value="false" />
    </bean>

We also use property files for 

Mail: ./main/resources/mail.properties
Twilio: ./main/resources/twilio.properties
Simple Notification Templates (Mail and SMS): ./main/resources/notification.properties
