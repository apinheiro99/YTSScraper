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

    // Função para extrair todos os detalhes do filme em uma única operação
    private static void extractMovieDetails(Element movie) {
        String movieName = movie.select(".browse-movie-title").text();
        String movieLink = movie.select(".browse-movie-link").attr("href"); // Link do filme

        String idiomaAbreviado = "[EN]";
        String idiomaExtenso = "English";

        if (movieName.startsWith("[")) {
            String abreviacaoIdioma = movieName.substring(0, movieName.indexOf("]") + 1);
            idiomaAbreviado = abreviacaoIdioma;
        }

        try {
            // Conectando à página interna do filme para obter mais detalhes
            Document movieDoc = Jsoup.connect(movieLink)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                    .get();

            // Pegando o título do filme na página interna
            Element movieTitleInternalElement = movieDoc.selectFirst("h1[itemprop=name]");
            String movieTitle = movieTitleInternalElement != null ? movieTitleInternalElement.text() : "Título desconhecido";

            // Pegando o ano e o idioma por extenso da página interna
            Elements h2Elements = movieDoc.select("h2");
            String year = h2Elements.size() > 0 ? h2Elements.get(0).text() : "Ano desconhecido";
            if (year.contains("[")) {
                String[] yearParts = year.split("\\[");
                year = yearParts[0].trim();
                idiomaExtenso = yearParts[1].replace("]", "").trim();
            }

            // Pegando o gênero do filme na página interna
            String genres = h2Elements.size() > 1 ? h2Elements.get(1).text() : "Gênero desconhecido";

            // Pegando a sinopse (na página interna)
            Element synopsisElement = movieDoc.selectFirst("#synopsis p");
            String synopsis = synopsisElement != null ? synopsisElement.text() : "Sinopse não disponível";

            // Pegando a duração do filme
            Element runtimeElement = movieDoc.selectFirst(".tech-spec-element .icon-clock");
            String runtime = runtimeElement != null ? runtimeElement.parent().text().trim() : "Duração desconhecida";

            // Pegando o elenco
            Element castElement = movieDoc.selectFirst(".actors .list-cast .name-cast");
            String cast = castElement != null ? castElement.text() : "Elenco desconhecido";

            // Pegando o diretor
            Element directorElement = movieDoc.selectFirst(".directors .list-cast .name-cast");
            String director = directorElement != null ? directorElement.text() : "Diretor desconhecido";

            // Pegando o link do trailer
            Element trailerElement = movieDoc.selectFirst("a.youtube");
            String trailerLink = trailerElement != null ? trailerElement.attr("href") : "Trailer não disponível";

            // Pegando a capa do filme
            Element coverElement = movieDoc.selectFirst("#movie-poster img");
            String movieCover = coverElement != null ? coverElement.attr("src") : "Capa não disponível";

            // Chamar o método para extrair resoluções
            String availableResolutions = extractResolutions(movieDoc);

            // Exibe os detalhes do filme, incluindo o link da página, URL da capa e o link do trailer
            System.out.println("Filme: " + movieTitle);
            System.out.println("Ano: " + year);
            System.out.println("Idioma: " + idiomaAbreviado + " " + capitalizeFirstLetter(idiomaExtenso));
            System.out.println("Gêneros: " + genres);
            System.out.println("Link da página: " + movieLink); // Link da página
            System.out.println("Capa: " + movieCover); // URL da capa
            System.out.println("Trailer: " + trailerLink); // Link do trailer
            System.out.println("Sinopse: " + synopsis);
            System.out.println("Duração: " + runtime);
            System.out.println("Elenco: " + cast);
            System.out.println("Diretor: " + director);
            System.out.println("Resoluções disponíveis:\n" + availableResolutions);
            System.out.println("-----------------------------------------------");

        } catch (IOException e) {
            System.out.println("Erro ao acessar a página do filme: " + movieLink);
            System.out.println("Motivo: " + e.getMessage());
        }
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
