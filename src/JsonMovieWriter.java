import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class JsonMovieWriter {
    private static final String JSON_FILE_PATH = "movies.json";
    private ObjectMapper objectMapper;
    private ObjectNode rootNode;

    public JsonMovieWriter() {
        objectMapper = new ObjectMapper();
        rootNode = loadJsonFile();
    }

    // Método para carregar o arquivo JSON ou criar um novo se ele não existir
    private ObjectNode loadJsonFile() {
        File jsonFile = new File(JSON_FILE_PATH);
        if (jsonFile.exists()) {
            try {
                return (ObjectNode) objectMapper.readTree(jsonFile);
            } catch (IOException e) {
                System.out.println("Erro ao carregar o arquivo JSON: " + e.getMessage());
            }
        }
        return objectMapper.createObjectNode();
    }

    // Método para salvar as alterações no arquivo JSON
    private void saveJsonFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(JSON_FILE_PATH), rootNode);
        } catch (IOException e) {
            System.out.println("Erro ao salvar o arquivo JSON: " + e.getMessage());
        }
    }

    // Método para adicionar ou atualizar um filme no JSON
    public void addOrUpdateMovie(String movieTitle, String year, String idiomaAbreviado, String idiomaExtenso,
                                 String[] genres, String movieLink, String movieCover, String trailerLink,
                                 String imdbLink, String imdbRating, String synopsis, String runtime,
                                 String cast, String director, String availableResolutions) {
        boolean isUpdated = false;
        ObjectNode movieNode = objectMapper.createObjectNode();
        movieNode.put("Ano", year);
        movieNode.put("Idioma", idiomaAbreviado + " " + capitalizeFirstLetter(idiomaExtenso));
        movieNode.put("Gêneros", String.join(" / ", genres));
        movieNode.put("Link da página", movieLink);
        movieNode.put("Capa", movieCover);
        movieNode.put("Trailer", trailerLink);
        movieNode.put("IMDb", imdbLink);
        movieNode.put("IMDb Rating", imdbRating);
        movieNode.put("Sinopse", synopsis);
        movieNode.put("Duração", runtime);
        movieNode.put("Elenco", cast);
        movieNode.put("Diretor", director);
        movieNode.put("Resoluções", availableResolutions);

        // Verificar se o filme já existe no JSON
        if (rootNode.has(movieTitle)) {
            ObjectNode existingMovie = (ObjectNode) rootNode.get(movieTitle);

            // Atualizar as informações do filme existente
            existingMovie.setAll(movieNode);
            isUpdated = true;
        } else {
            // Adicionar um novo filme ao JSON
            rootNode.set(movieTitle, movieNode);
        }

        // Salvar as alterações no arquivo JSON
        saveJsonFile();

        // Mostrar mensagem de status da atualização no método printMovieDetails
        if (isUpdated) {
            System.out.println("Filme: " + movieTitle + " (Updated)");
        } else {
            printFullMovieDetails(movieTitle, year, idiomaAbreviado, idiomaExtenso, genres, movieLink, movieCover,
                    trailerLink, imdbLink, imdbRating, synopsis, runtime, cast, director, availableResolutions);
        }
    }

    // Método auxiliar para capitalizar a primeira letra de uma string
    private static String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    // Método para imprimir todos os detalhes do filme se ele for novo
    private static void printFullMovieDetails(String movieTitle, String year, String idiomaAbreviado, String idiomaExtenso,
                                              String[] genres, String movieLink, String movieCover, String trailerLink,
                                              String imdbLink, String imdbRating, String synopsis, String runtime,
                                              String cast, String director, String availableResolutions) {
        System.out.println("Filme: " + movieTitle);
        System.out.println("Ano: " + year);
        System.out.println("Idioma: " + idiomaAbreviado + " " + capitalizeFirstLetter(idiomaExtenso));
        System.out.println("Gêneros: " + String.join(" / ", genres));
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
}
