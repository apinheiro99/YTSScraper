import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;

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
                                 String cast, String director, ArrayNode availableResolutions) {
        boolean isUpdated = false;
        ObjectNode movieNode = objectMapper.createObjectNode();

        // Adicionando "Ano"
        movieNode.put("Ano", year);

        // Adicionando "Idioma" como um array
        ArrayNode idiomaArray = objectMapper.createArrayNode();
        idiomaArray.add(idiomaAbreviado);
        idiomaArray.add(idiomaExtenso);
        movieNode.set("Idioma", idiomaArray);

        // Adicionando "Gêneros" como um array
        ArrayNode genresArray = objectMapper.createArrayNode();
        for (String genre : genres) {
            genresArray.add(genre);
        }
        movieNode.set("Gêneros", genresArray);

        // Adicionando outros detalhes do filme
        movieNode.put("Link da página", movieLink);
        movieNode.put("Capa", movieCover);
        movieNode.put("Trailer", trailerLink);
        movieNode.put("IMDb", imdbLink);
        movieNode.put("IMDb Rating", imdbRating);
        movieNode.put("Sinopse", synopsis);
        movieNode.put("Duração", runtime);
        movieNode.put("Elenco", cast);
        movieNode.put("Diretor", director);
        movieNode.set("Resoluções", availableResolutions);

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
            System.out.println("Filme: " + movieTitle + " foi adicionado.");
        }
    }
}
