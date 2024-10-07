import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YTSScraper {

    public static void main(String[] args) {
        String baseURL = "https://yts.mx/browse-movies/0/all/all/0/year/0/all";
        String paginatedURL = "https://yts.mx/browse-movies/0/all/all/0/year/0/all?page=";

        int pageNumber = 1;
        boolean hasNextPage = true;

        try {
            hasNextPage = scrapeAllMovies(baseURL);

            pageNumber++;
            while (hasNextPage) {
                String url = paginatedURL + pageNumber;
                hasNextPage = scrapeAllMovies(url);
                pageNumber++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean scrapeAllMovies(String url) throws IOException {
        Document doc = accessPageWithRetry(url, 3);
        if (doc == null) {
            System.out.println("Falha ao acessar a página: " + url);
            return false;
        }

        Elements movies = doc.select(".browse-movie-wrap");

        if (movies.isEmpty()) {
            System.out.println("Nenhum filme encontrado na página: " + url);
            return false;
        }

        for (Element movie : movies) {
            extractMovieDetails(movie);
        }

        return true;
    }

    private static void extractMovieDetails(Element movie) {
        String movieName = movie.select(".browse-movie-title").text();
        String movieLink = movie.select(".browse-movie-link").attr("href");

        String idiomaAbreviado = getIdiomaAbreviado(movieName);
        String[] idiomaExtensoHolder = new String[]{"English"};

        try {
            Document movieDoc = connectToMoviePage(movieLink);

            String movieTitle = extractMovieTitle(movieDoc);
            String year = extractYearAndIdioma(movieDoc, idiomaExtensoHolder);
            String idiomaExtenso = idiomaExtensoHolder[0];
            String[] genres = extractGenres(movieDoc);
            String synopsis = extractSynopsis(movieDoc);
            String runtime = extractRuntime(movieDoc);
            String cast = extractCast(movieDoc);
            String director = extractDirector(movieDoc);
            String trailerLink = extractTrailerLink(movieDoc);
            String movieCover = extractMovieCover(movieDoc);
            String imdbLink = extractImdbLink(movieDoc);
            String imdbId = extractImdbId(imdbLink); // Extraímos o código IMDb aqui
            String imdbRating = fetchImdbOrYtsRating(imdbLink, movieDoc);

            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode availableResolutions = extractResolutions(movieDoc, objectMapper);

            JsonMovieWriter jsonMovieWriter = new JsonMovieWriter();
            jsonMovieWriter.addOrUpdateMovie(imdbId, movieTitle, year, idiomaAbreviado, idiomaExtenso, genres, movieLink,
                    movieCover, trailerLink, imdbLink, imdbRating, synopsis, runtime, cast, director, availableResolutions);
        } catch (IOException e) {
            System.out.println("Erro ao acessar a página do filme: " + movieLink);
            System.out.println("Motivo: " + e.getMessage());
        }
    }

    private static String extractImdbId(String imdbLink) {
        if (imdbLink != null && !imdbLink.isEmpty()) {
            Pattern pattern = Pattern.compile("tt\\d+");
            Matcher matcher = pattern.matcher(imdbLink);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return "N/A";
    }

    private static Document accessPageWithRetry(String url, int maxRetries) throws IOException {
        int retries = 0;
        Document doc = null;

        while (retries < maxRetries) {
            try {
                doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                        .timeout(10 * 1000).get();
                break;
            } catch (IOException e) {
                retries++;
                System.out.println("Tentativa " + retries + " falhou para " + url);
                if (retries >= maxRetries) {
                    throw e;
                }
            }
        }
        return doc;
    }

    private static String getIdiomaAbreviado(String movieName) {
        if (movieName.startsWith("[")) {
            return movieName.substring(0, movieName.indexOf("]") + 1);
        }
        return "[EN]";
    }

    private static Document connectToMoviePage(String movieLink) throws IOException {
        return Jsoup.connect(movieLink)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                .get();
    }

    private static String extractMovieTitle(Document movieDoc) {
        Element movieTitleInternalElement = movieDoc.selectFirst("h1[itemprop=name]");
        return movieTitleInternalElement != null ? movieTitleInternalElement.text() : "N/A";
    }

    private static String extractYearAndIdioma(Document movieDoc, String[] idiomaExtensoHolder) {
        Elements h2Elements = movieDoc.select("h2");
        String year = h2Elements.size() > 0 ? h2Elements.get(0).text() : "N/A";
        if (year.contains("[")) {
            String[] yearParts = year.split("\\[");
            idiomaExtensoHolder[0] = yearParts[1].replace("]", "").trim();
            return yearParts[0].trim();
        }
        return year;
    }

    private static String[] extractGenres(Document movieDoc) {
        Elements h2Elements = movieDoc.select("h2");
        String genres = h2Elements.size() > 1 ? h2Elements.get(1).text() : "N/A";
        return genres.equals("N/A") ? new String[]{"N/A"} : genres.split("\\s*/\\s*");
    }

    private static String extractSynopsis(Document movieDoc) {
        Element synopsisElement = movieDoc.selectFirst("#synopsis p");
        return synopsisElement != null ? synopsisElement.text() : "N/A";
    }

    private static String extractRuntime(Document movieDoc) {
        Element runtimeElement = movieDoc.selectFirst(".tech-spec-element .icon-clock");
        return runtimeElement != null ? runtimeElement.parent().text().trim() : "N/A";
    }

    private static String extractCast(Document movieDoc) {
        Element castElement = movieDoc.selectFirst(".actors .list-cast .name-cast");
        return castElement != null ? castElement.text() : "N/A";
    }

    private static String extractDirector(Document movieDoc) {
        Element directorElement = movieDoc.selectFirst(".directors .list-cast .name-cast");
        return directorElement != null ? directorElement.text() : "N/A";
    }

    private static String extractTrailerLink(Document movieDoc) {
        Element trailerElement = movieDoc.selectFirst("a.youtube");
        return trailerElement != null ? trailerElement.attr("href") : "N/A";
    }

    private static String extractMovieCover(Document movieDoc) {
        Element coverElement = movieDoc.selectFirst("#movie-poster img");
        return coverElement != null ? coverElement.attr("src") : "N/A";
    }

    private static String extractImdbLink(Document movieDoc) {
        Element imdbLinkElement = movieDoc.selectFirst("a[title='IMDb Rating']");
        return imdbLinkElement != null ? imdbLinkElement.attr("href") : "N/A";
    }

    private static String fetchImdbOrYtsRating(String imdbLink, Document movieDoc) {
        String imdbRating = "N/A";
        if (!imdbLink.equals("N/A")) {
            imdbRating = fetchImdbRating(imdbLink);
        }
        if (imdbRating.equals("N/A")) {
            imdbRating = extractYtsRating(movieDoc);
        }
        return imdbRating;
    }

    private static String fetchImdbRating(String imdbLink) {
        try {
            Document imdbDoc = Jsoup.connect(imdbLink)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                    .get();
            Element imdbRatingElement = imdbDoc.selectFirst("div[data-testid='hero-rating-bar__aggregate-rating__score'] span.sc-d541859f-1");
            return imdbRatingElement != null ? imdbRatingElement.text() : "N/A";
        } catch (IOException e) {
            System.out.println("Erro ao acessar a página do IMDb: " + imdbLink);
            System.out.println("Motivo: " + e.getMessage());
            return "N/A";
        }
    }

    private static String extractYtsRating(Document movieDoc) {
        Element ytsRatingElement = movieDoc.selectFirst("#movie-likes");
        return ytsRatingElement != null ? ytsRatingElement.text() : "N/A";
    }

    private static ArrayNode extractResolutions(Document movieDoc, ObjectMapper objectMapper) {
        Elements resolutionElements = movieDoc.select(".modal-torrent");

        ArrayNode resolutionsArray = objectMapper.createArrayNode();
        String fileSizeRegex = "(\\d+(?:\\.\\d+)?\\s(?:GB|MB))";

        for (Element resolutionElement : resolutionElements) {
            ObjectNode resolutionNode = objectMapper.createObjectNode();
            String resolution = resolutionElement.selectFirst(".modal-quality span").text();
            resolutionNode.put("Qualidade", resolution);

            String concatenatedQualities = resolutionElement.select(".quality-size").text();
            Pattern pattern = Pattern.compile(fileSizeRegex);
            Matcher matcher = pattern.matcher(concatenatedQualities);
            String fileSize = "";

            if (matcher.find()) {
                fileSize = matcher.group(1);
                concatenatedQualities = concatenatedQualities.replace(fileSize, "").trim();
            }
            resolutionNode.put("Tamanho do arquivo", fileSize);

            ArrayNode technicalDetailsArray = objectMapper.createArrayNode();
            String[] qualities = concatenatedQualities.split("\\.");
            for (String part : qualities) {
                technicalDetailsArray.add(part.trim());
            }
            resolutionNode.set("Detalhes técnicos", technicalDetailsArray);

            String torrentLink = resolutionElement.selectFirst(".download-torrent").attr("href");
            String magnetLink = resolutionElement.selectFirst(".magnet-download").attr("href");
            resolutionNode.put("Link Torrent", torrentLink);
            resolutionNode.put("Magnet", magnetLink);

            resolutionsArray.add(resolutionNode);
        }

        return resolutionsArray;
    }
}
