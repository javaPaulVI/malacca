package io.github.javapaulvi.malacca.http.responses;

public class RedirectResponse extends Response<Void> {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("RedirectResponse");
    public RedirectResponse(String address) {
        super(null);
        status(302).header("Location", address);
        if (address.startsWith("http://")) {
            logger.warn("RedirectResponse is using HTTP instead of HTTPS — consider using HTTPS in production");
        }
    }
}
