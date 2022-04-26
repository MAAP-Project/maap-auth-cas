import org.apereo.cas.util.spring.*

def Map<String, Object> run(final Object... args) {
    def username = args[0]
    def attributes = args[1]
    def logger = args[2]
    def properties = args[3]
    def appContext = args[4]
    boolean esa = false

    logger.debug("Running Groovy script: cas/etc/cas/attributes.groovy")

    attributes.each{ key, value -> 
        logger.debug("key: " + key + " value: " + value)
        if (key.equals("iss")) {
            esa = true
        }
    }

    if(esa) {
        //ESA client found. Add 'status=active' attribute to profile. ESA users are assumed to be pre-approved.
        return["status":["active"]]
    } else {
        return[]
    }
}