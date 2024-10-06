import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class YTSScraper {

    public static void main(String[] args) {
        // URL base para a navegação de páginas
        String baseURL = "https://yts.mx/browse-movies/0/all/all/0/year/0/all";
        String paginatedURL = "https://yts.mx/browse-movies/0/all/all/0/year/0/all?page=";

        int pageNumber = 1;
        boolean hasNextPage = true;

        try {
            // Processar a primeira página
            System.out.println("Processando página: " + pageNumber);
            hasNextPage = scrapeAllMovies(baseURL);

            // Continuar para as próximas páginas, onde a URL inclui o parâmetro ?page=
            pageNumber++;
            while (hasNextPage) {
                String url = paginatedURL + pageNumber;
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

        // Para cada filme, capturar o nome, ano, gênero e detalhes
        for (Element movie : movies) {
            String movieName = movie.select(".browse-movie-title").text();
            String movieYear = movie.select(".browse-movie-year").text();
            String movieLink = movie.select(".browse-movie-link").attr("href");

            // Exibir o nome do filme e ano no console
            System.out.println("Filme encontrado: " + movieName + " (" + movieYear + ")");

            // Acessar a página interna de cada filme para pegar mais detalhes
            scrapeMovieDetails(movieLink);
        }

        // Verificar se há um botão ou link "Next" para continuar para a próxima página
        Element nextButton = doc.selectFirst(".pagination .next");
        return nextButton != null && nextButton.hasAttr("href");
    }

    // Função para acessar a página do filme e pegar os detalhes adicionais
    private static void scrapeMovieDetails(String moviePageUrl) throws IOException {
        Document movieDoc = Jsoup.connect(moviePageUrl).get();

        // Pegando o título do filme
        String movieTitle = movieDoc.selectFirst("h1[itemprop=name]").text();

        // Pegando o ano do filme
        String year = movieDoc.select("h2").get(0).text(); // O primeiro <h2> contém o ano

        // Pegando o gênero do filme
        String genres = movieDoc.select("h2").get(1).text(); // O segundo <h2> contém o gênero

        // Pegando a sinopse correta da página
        Element synopsisElement = movieDoc.selectFirst("#synopsis p");
        String synopsis = synopsisElement != null ? synopsisElement.text() : "Sinopse não disponível";

        // Pegando a duração do filme
        Element runtimeElement = movieDoc.selectFirst(".tech-spec-element .icon-clock");
        String runtime = runtimeElement != null ? runtimeElement.parent().text().trim() : "Duração desconhecida";

        // Pegando o diretor
        Element directorElement = movieDoc.selectFirst(".directors .list-cast .name-cast");
        String director = directorElement != null ? directorElement.text() : "Diretor desconhecido";

        // Pegando o elenco
        Element castElement = movieDoc.selectFirst(".actors .list-cast .name-cast");
        String cast = castElement != null ? castElement.text() : "Elenco desconhecido";

        // Exibir os detalhes do filme no console
        System.out.println("Filme encontrado: " + movieTitle);
        System.out.println("Ano: " + year);
        System.out.println("Gêneros: " + genres);
        System.out.println("Sinopse: " + synopsis);
        System.out.println("Duração: " + runtime);
        System.out.println("Diretor: " + director);
        System.out.println("Elenco: " + cast);
        System.out.println("-----------------------------------------------");
    }

    // Função para tentar acessar uma página com retries
    private static Document accessPageWithRetry(String url, int maxRetries) throws IOException {
        int retries = 0;
        Document doc = null;

        while (retries < maxRetries) {
            try {
                doc = Jsoup.connect(url).timeout(10 * 1000).get();
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
