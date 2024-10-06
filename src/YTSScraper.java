import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YTSScraper {

    public static void main(String[] args) {
        // URL base para a primeira página
        String baseURL = "https://yts.mx/browse-movies/0/all/all/0/year/0/all";
        String paginatedURL = "https://yts.mx/browse-movies/0/all/all/0/year/0/all?page=";

        int pageNumber = 1;
        boolean hasNextPage = true;

        try {
            // Processar a primeira página
            hasNextPage = scrapeAllMovies(baseURL);

            // Processar as próximas páginas diretamente pela URL paginada
            pageNumber++;
            while (hasNextPage) {
                String url = paginatedURL + pageNumber; // Usar paginatedURL para as próximas páginas
                hasNextPage = scrapeAllMovies(url);
                pageNumber++; // Incrementar o número da página
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Função para acessar uma página e processar todos os filmes nela
    private static boolean scrapeAllMovies(String url) throws IOException {
        Document doc = accessPageWithRetry(url, 3);
        if (doc == null) {
            System.out.println("Falha ao acessar a página: " + url);
            return false;
        }

        // Seleciona todos os elementos de filmes na página
        Elements movies = doc.select(".browse-movie-wrap");

        // Se não houver filmes na página, retorne false
        if (movies.isEmpty()) {
            System.out.println("Nenhum filme encontrado na página: " + url);
            return false;
        }

        // Para cada filme, processar e extrair detalhes
        for (Element movie : movies) {
            extractMovieDetails(movie);
        }

        // Se filmes foram encontrados, continuar para a próxima página
        return true;
    }

    // Função principal para extrair detalhes do filme
    private static void extractMovieDetails(Element movie) {
        String movieName = movie.select(".browse-movie-title").text();
        String movieLink = movie.select(".browse-movie-link").attr("href");

        String idiomaAbreviado = getIdiomaAbreviado(movieName);
        String idiomaExtenso = "English"; // Valor padrão para idioma extenso

        try {
            Document movieDoc = connectToMoviePage(movieLink);

            String movieTitle = extractMovieTitle(movieDoc);
            String year = extractYearAndIdioma(movieDoc, idiomaExtenso);
            String genres = extractGenres(movieDoc);
            String synopsis = extractSynopsis(movieDoc);
            String runtime = extractRuntime(movieDoc);
            String cast = extractCast(movieDoc);
            String director = extractDirector(movieDoc);
            String trailerLink = extractTrailerLink(movieDoc);
            String movieCover = extractMovieCover(movieDoc);
            String imdbLink = extractImdbLink(movieDoc);
            String imdbRating = fetchImdbOrYtsRating(imdbLink, movieDoc);
            String availableResolutions = extractResolutions(movieDoc);

            printMovieDetails(movieTitle, year, idiomaAbreviado, idiomaExtenso, genres, movieLink, movieCover,
                    trailerLink, imdbLink, imdbRating, synopsis, runtime, cast, director, availableResolutions);
        } catch (IOException e) {
            System.out.println("Erro ao acessar a página do filme: " + movieLink);
            System.out.println("Motivo: " + e.getMessage());
        }
    }

// Funções auxiliares para cada extração

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

    private static String extractYearAndIdioma(Document movieDoc, String idiomaExtenso) {
        Elements h2Elements = movieDoc.select("h2");
        String year = h2Elements.size() > 0 ? h2Elements.get(0).text() : "N/A";
        if (year.contains("[")) {
            String[] yearParts = year.split("\\[");
            idiomaExtenso = yearParts[1].replace("]", "").trim();
            return yearParts[0].trim();
        }
        return year;
    }

    private static String extractGenres(Document movieDoc) {
        Elements h2Elements = movieDoc.select("h2");
        return h2Elements.size() > 1 ? h2Elements.get(1).text() : "N/A";
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

    private static void printMovieDetails(String movieTitle, String year, String idiomaAbreviado, String idiomaExtenso,
                                          String genres, String movieLink, String movieCover, String trailerLink,
                                          String imdbLink, String imdbRating, String synopsis, String runtime,
                                          String cast, String director, String availableResolutions) {
        System.out.println("Filme: " + movieTitle);
        System.out.println("Ano: " + year);
        System.out.println("Idioma: " + idiomaAbreviado + " " + capitalizeFirstLetter(idiomaExtenso));
        System.out.println("Gêneros: " + genres);
        System.out.println("Link da página: " + movieLink);
        System.out.println("Capa: " + movieCover);
        System.out.println("Trailer: " + trailerLink);
        System.out.println("IMDb: " + imdbLink);
        System.out.println("IMDb Rating: " + imdbRating);
        System.out.println("Sinopse: " + synopsis);
        System.out.println("Duração: " + runtime);
        System.out.println("Elenco: " + cast);
        System.out.println("Diretor: " + director);
        System.out.println("Resoluções:\n" + availableResolutions);
        System.out.println("-----------------------------------------------");
    }

    // Método para extrair resoluções e retorná-las em formato String
    private static String extractResolutions(Document movieDoc) {
        Elements resolutionElements = movieDoc.select(".modal-torrent");

        StringBuilder resolutions = new StringBuilder();

        // Regex para identificar o tamanho do arquivo (ex: "5.54 GB" ou "814.81 MB")
        String fileSizeRegex = "(\\d+(?:\\.\\d+)?\\s(?:GB|MB))";

        for (Element resolutionElement : resolutionElements) {
            // Resolução principal (720p, 1080p, etc.)
            String resolution = resolutionElement.selectFirst(".modal-quality span").text(); // Ex: 720p, 1080p

            // Pegamos todos os valores concatenados por ponto, como "WEB.x265.10bit"
            String concatenatedQualities = resolutionElement.select(".quality-size").text(); // Ex: "WEB.x265.10bit.5.54 GB"

            // Extraímos o tamanho do arquivo usando regex
            Pattern pattern = Pattern.compile(fileSizeRegex);
            Matcher matcher = pattern.matcher(concatenatedQualities);
            String fileSize = "";

            // Verifica se encontramos o tamanho do arquivo
            if (matcher.find()) {
                fileSize = matcher.group(1); // Captura o tamanho do arquivo (ex: "5.54 GB")
                // Removemos o tamanho do arquivo da string concatenada para evitar problemas
                concatenatedQualities = concatenatedQualities.replace(fileSize, "").trim();
            }

            // Dividimos os valores restantes com base no ponto, agora sem o tamanho do arquivo
            String[] qualities = concatenatedQualities.split("\\.");

            // Loop para adicionar cada parte ao vetor dinâmico
            StringBuilder qualityDetails = new StringBuilder(resolution);
            for (String part : qualities) {
                qualityDetails.append(" ").append(part);
            }

            // Adiciona o tamanho do arquivo
            if (!fileSize.isEmpty()) {
                qualityDetails.append(" (").append(fileSize).append(")");
            }

            // Links
            String torrentLink = resolutionElement.selectFirst(".download-torrent").attr("href");
            String magnetLink = resolutionElement.selectFirst(".magnet-download").attr("href");

            // Formata o resultado final
            String formattedDetails = qualityDetails.toString() + "\n"
                    + "Link Torrent: " + torrentLink + "\n"
                    + "Magnet: " + magnetLink + "\n";

            // Adiciona ao StringBuilder
            resolutions.append(formattedDetails).append("\n");
        }

        return resolutions.length() > 0 ? resolutions.toString().trim() : "Resoluções desconhecidas";
    }

    // Método auxiliar para capitalizar a primeira letra de uma string
    private static String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    // Função para tentar acessar uma página com retries
    private static Document accessPageWithRetry(String url, int maxRetries) throws IOException {
        int retries = 0;
        Document doc = null;

        while (retries < maxRetries) {
            try {
                doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                        .timeout(10 * 1000).get();
                break; // Sucesso, sai do loop
            } catch (IOException e) {
                retries++;
                System.out.println("Tentativa " + retries + " falhou para " + url);
                if (retries >= maxRetries) {
                    throw e; // Lança exceção se atingir o número máximo de tentativas
                }
            }
        }
        return doc;
    }
}
