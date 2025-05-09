package nl.tno.essim.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ResourceUtils {
    public static List<String> getResourcePaths(String path) throws IOException {
        List<String> filenames = new ArrayList<>();

        try (InputStream in = ResourceUtils.class.getResourceAsStream(path); BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;
            while ((resource = br.readLine()) != null) {
                filenames.add(path + "/" + resource);
            }
        }
        return filenames;
    }

}
