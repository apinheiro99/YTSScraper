import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class YTSScraper {

    public static void main(String[] args) {
        // URL base para a primeira página
        String baseURL = "https://yts.mx/browse-movies/0/all/all/0/year/0/all";
        String paginatedURL = "https://yts.mx/browse-movies/0/all/all/0/year/0/all?page=";

        int pageNumber = 1;
        boolean hasNextPage = true;

        try {
            // Processar a primeira página
            System.out.println("Processando página: 1");
            hasNextPage = scrapeAllMovies(baseURL);

            // Processar as próximas páginas diretamente pela URL paginada
            pageNumber++;
            while (hasNextPage) {
                String url = paginatedURL + pageNumber; // Usar paginatedURL para as próximas páginas
                System.out.println("Processando página: " + pageNumber);
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
        String movieLink = movie.select(".browse-movie-link").attr("href");

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

            // Captura as resoluções, tamanhos de arquivos e links
            Elements resolutionElements = movieDoc.select(".modal-torrent");

            StringBuilder resolutions = new StringBuilder();
            for (Element resolutionElement : resolutionElements) {
                String resolution = resolutionElement.selectFirst(".modal-quality span").text(); // Ex: 720p, 1080p
                String fileSize = resolutionElement.select(".quality-size").get(1).text(); // Ex: 912.56 MB, 1.83 GB
                String torrentLink = resolutionElement.selectFirst(".download-torrent").attr("href"); // Link para o torrent
                String magnetLink = resolutionElement.selectFirst(".magnet-download").attr("href"); // Link magnet

                // Formatação da saída de resoluções e links
                resolutions.append(resolution).append(" (").append(fileSize).append(")\n");
                resolutions.append("Link Torrent: ").append(torrentLink).append("\n");
                resolutions.append("Magnet: ").append(magnetLink).append("\n\n");
            }

            String availableResolutions = resolutions.length() > 0 ? resolutions.toString().trim() : "Resoluções desconhecidas";

            // Exibe os detalhes do filme
            System.out.println("Filme: " + movieTitle);
            System.out.println("Ano: " + year);
            System.out.println("Idioma: " + idiomaAbreviado + " " + capitalizeFirstLetter(idiomaExtenso));
            System.out.println("Gêneros: " + genres);
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



    // Função auxiliar para capitalizar a primeira letra e deixar o restante em minúsculas
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
