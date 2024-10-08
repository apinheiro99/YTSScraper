import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JsonMovieWriter {
    private static final String JSON_FILE_PATH = "movies.json";
    private ObjectMapper objectMapper;
    private ObjectNode rootNode;

    public JsonMovieWriter() {
        objectMapper = new ObjectMapper();
        rootNode = loadJsonFile();
    }

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

    private void saveJsonFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(JSON_FILE_PATH), rootNode);
        } catch (IOException e) {
            System.out.println("Erro ao salvar o arquivo JSON: " + e.getMessage());
        }
    }

    public void addOrUpdateMovie(String imdbId, String movieTitle, String year, String idiomaAbreviado, String idiomaExtenso,
                                 String[] genres, String movieLink, String movieCover, String trailerLink,
                                 String imdbLink, String imdbRating, String synopsis, String runtime,
                                 String cast, String director, ArrayNode availableResolutions) {
        boolean isUpdated = false;
        boolean hasChanges = false;
        boolean isNewMovie = false;
        List<String> updatedAttributes = new ArrayList<>();

        int existingUpdateCount = 0; // Contagem de atualizações existente

        // Verifica se o filme já existe no JSON e obtém a contagem de atualizações existente
        if (rootNode.has(imdbId)) {
            ObjectNode existingMovie = (ObjectNode) rootNode.get(imdbId);
            existingUpdateCount = existingMovie.has("Contagem de Atualizações") ?
                    existingMovie.get("Contagem de Atualizações").asInt() : 0;

            // Cria o novo nó de filme com a contagem de atualizações existente
            ObjectNode movieNode = initializeMovieNode(movieTitle, year, idiomaAbreviado, idiomaExtenso, genres, movieLink,
                    movieCover, trailerLink, imdbLink, imdbRating, synopsis, runtime, cast, director, availableResolutions,
                    existingUpdateCount);

            hasChanges = detectChanges(existingMovie, movieNode, updatedAttributes); // Detecta mudanças sem logs detalhados

            if (hasChanges) {
                updateExistingMovie(existingMovie, movieNode);
                isUpdated = true;
            }
        } else {
            // Cria o novo nó de filme para um filme que não existe no JSON
            ObjectNode movieNode = initializeMovieNode(movieTitle, year, idiomaAbreviado, idiomaExtenso, genres, movieLink,
                    movieCover, trailerLink, imdbLink, imdbRating, synopsis, runtime, cast, director, availableResolutions,
                    existingUpdateCount);

            rootNode.set(imdbId, movieNode);
            isNewMovie = true;
        }

        saveJsonFile();
        displayUpdateStatus(movieTitle, isUpdated, hasChanges, isNewMovie, updatedAttributes);
    }

    private ObjectNode initializeMovieNode(String movieTitle, String year, String idiomaAbreviado, String idiomaExtenso,
                                           String[] genres, String movieLink, String movieCover, String trailerLink,
                                           String imdbLink, String imdbRating, String synopsis, String runtime,
                                           String cast, String director, ArrayNode availableResolutions,
                                           int existingUpdateCount) { // Novo parâmetro para contagem de atualizações existente
        ObjectNode movieNode = objectMapper.createObjectNode();
        movieNode.put("Título", movieTitle);
        movieNode.put("Ano", year);

        ArrayNode idiomaArray = objectMapper.createArrayNode();
        idiomaArray.add(idiomaAbreviado);
        idiomaArray.add(idiomaExtenso);
        movieNode.set("Idioma", idiomaArray);

        ArrayNode genresArray = objectMapper.createArrayNode();
        for (String genre : genres) {
            genresArray.add(genre);
        }
        movieNode.set("Gêneros", genresArray);

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

        // Use a contagem de atualizações existente, se fornecida; caso contrário, inicie com 0 para novos filmes
        movieNode.put("Contagem de Atualizações", existingUpdateCount);

        return movieNode;
    }

    private boolean detectChanges(ObjectNode existingMovie, ObjectNode movieNode, List<String> updatedAttributes) {
        boolean hasChanges = false;
        Iterator<String> fieldNames = movieNode.fieldNames();

        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (existingMovie.has(fieldName)) {
                JsonNode existingValueNode = existingMovie.get(fieldName);
                JsonNode newValueNode = movieNode.get(fieldName);

                // Comparação direta de valores para todos os campos
                if (!existingValueNode.equals(newValueNode)) {
                    updatedAttributes.add(fieldName);
                    hasChanges = true;
                }
            }
        }
        return hasChanges;
    }

    private void updateExistingMovie(ObjectNode existingMovie, ObjectNode movieNode) {
        existingMovie.setAll(movieNode);

        // Incrementa a contagem de atualizações somente se houver modificações reais
        if (existingMovie.has("Contagem de Atualizações")) {
            incrementUpdateCount(existingMovie);
        } else {
            existingMovie.put("Contagem de Atualizações", 1); // Inicializar contagem de atualizações se não existir
        }
    }

    private void incrementUpdateCount(ObjectNode movieNode) {
        int updateCount = movieNode.has("Contagem de Atualizações") ? movieNode.get("Contagem de Atualizações").asInt() : 0;
        movieNode.put("Contagem de Atualizações", updateCount + 1);
    }

    private void displayUpdateStatus(String movieTitle, boolean isUpdated, boolean hasChanges, boolean isNewMovie, List<String> updatedAttributes) {
        if (isNewMovie) {
            System.out.println("Filme: " + movieTitle + "  -->  Adicionado.");
        } else if (isUpdated) {
            System.out.println("Filme: " + movieTitle + "  -->  Atributos modificados: " + String.join(", ", updatedAttributes));
        } else {
            System.out.println("Filme: " + movieTitle + "  -->  Sem modificações.");
        }
    }
}
