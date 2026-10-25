package framework;

import java.util.Locale;
import java.util.Objects;

public final class UrlMethode {
    private final String url;
    private final String methode;

    public UrlMethode(String url, String methode) {
        this.url = Objects.requireNonNull(url, "url");
        this.methode = Objects.requireNonNull(methode, "methode").toUpperCase(Locale.ROOT);
    }

    public String getUrl() {
        return url;
    }

    public String getMethode() {
        return methode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UrlMethode)) {
            return false;
        }
        UrlMethode that = (UrlMethode) other;
        return url.equals(that.url) && methode.equals(that.methode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, methode);
    }
}
