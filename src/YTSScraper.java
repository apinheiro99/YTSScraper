import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
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

    private static void extractMovieDetails(Element movie) throws IOException {
        // Captura o nome do filme e o link da página principal
        String movieName = movie.select(".browse-movie-title").text();
        String movieLink = movie.select(".browse-movie-link").attr("href");

        // Inicializa o idioma padrão como inglês (abreviação)
        String idiomaAbreviado = "[EN]";
        String idiomaExtenso = "English";

        // Verifica se o nome do filme contém colchetes e captura a abreviação do idioma
        if (movieName.startsWith("[")) {
            String abreviacaoIdioma = movieName.substring(0, movieName.indexOf("]") + 1); // Exemplo: [ES], [PT]
            idiomaAbreviado = abreviacaoIdioma;
        }

        // Conectando à página interna do filme para obter mais detalhes, incluindo o idioma por extenso
        Document movieDoc = Jsoup.connect(movieLink)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                .get();

        // Pegando o título do filme na página interna
        Element movieTitleInternalElement = movieDoc.selectFirst("h1[itemprop=name]");
        String movieTitle = movieTitleInternalElement != null ? movieTitleInternalElement.text() : "Título desconhecido";

        // Pegando o ano e o idioma por extenso da página interna
        Elements h2Elements = movieDoc.select("h2");
        String year = h2Elements.size() > 0 ? h2Elements.get(0).text() : "Ano desconhecido";

        // Verifica se o ano contém colchetes e captura o idioma por extenso
        if (year.contains("[")) {
            String[] yearParts = year.split("\\[");  // Divide o ano e o idioma
            year = yearParts[0].trim();  // Mantém o ano puro
            idiomaExtenso = yearParts[1].replace("]", "").trim();  // Captura o idioma por extenso (ex: Spanish, Hindi)
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

        // Exibir todos os detalhes do filme no console
        System.out.println("Filme: " + movieTitle);
        System.out.println("Ano: " + year);
        System.out.println("Idioma: " + idiomaAbreviado + " " + capitalizeFirstLetter(idiomaExtenso));
        System.out.println("Gêneros: " + genres);
        System.out.println("Sinopse: " + synopsis);
        System.out.println("Duração: " + runtime);
        System.out.println("Elenco: " + cast);
        System.out.println("Diretor: " + director);
        System.out.println("-----------------------------------------------");
    }


    // Função para salvar o HTML bruto em um arquivo .txt para futura análise
    private static void saveRawPage(String movieLink, String movieName) {
        try {
            Document movieDoc = Jsoup.connect(movieLink)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                    .get();

            // Define o nome do arquivo com base no nome do filme
            String fileName = "raw_" + movieName.replaceAll("[^a-zA-Z0-9]", "_") + ".txt";

            try (FileWriter fileWriter = new FileWriter(fileName)) {
                fileWriter.write(movieDoc.outerHtml());
                System.out.println("Página raw salva para análise: " + fileName);
            }
        } catch (IOException e) {
            System.out.println("Erro ao salvar a página raw: " + e.getMessage());
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
