
package jline.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class Urls {
    public static URL create(String input) {
        if (input == null) {
            return null;
        }
        try {
            return new URL(input);
        }
        catch (MalformedURLException e) {
            return Urls.create(new File(input));
        }
    }

    public static URL create(File file) {
        try {
            return file != null ? file.toURI().toURL() : null;
        }
        catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }
}

