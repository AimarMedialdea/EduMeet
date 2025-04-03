package uni.paag2.myapplication.supabase;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SupabaseHelper {
    private static final String BASE_URL = "https://trjiewwhjoeytkdwkvlm.supabase.co/rest/v1/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRyamlld3doam9leXRrZHdrdmxtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDM0OTY1ODQsImV4cCI6MjA1OTA3MjU4NH0.YS6EF001LPQq4RyJEGLLbQc8DSu4lidDRQMAjbjBOrw";

    private final OkHttpClient client;

    public SupabaseHelper() {
        this.client = new OkHttpClient();
    }

    public interface SupabaseCallback {
        void onSuccess(String response);
        void onFailure(String error);
    }




    // Método para insertar reunión en Supabase
    public void insertarReunion(int idProfesor, String horaInicio, String tema, SupabaseCallback callback) {
        String url = BASE_URL + "reunion";

        JSONObject json = new JSONObject();
        try {
            json.put("id_profesor", idProfesor);
            json.put("hora_inicio", horaInicio);
            json.put("tema", tema);
        } catch (Exception e) {
            callback.onFailure("Error al crear JSON");
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess("Reunión insertada correctamente");
                } else {
                    callback.onFailure("Error: " + response.code() + " " + response.message());
                }
            }
        });
    }

    // Método para obtener todos los profesores
    public void obtenerNombresProfesores(SupabaseCallback callback) {
        String url = BASE_URL + "profesor?select=id_profesor,nombre";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    callback.onSuccess(responseData);
                } else {
                    callback.onFailure("Error: " + response.code() + " " + response.message());
                }
            }
        });
    }

    // Método para insertar una reunión con múltiples participantes
    public void insertarReunionConParticipantes(String tema, String horaInicio, List<Integer> idProfesores, SupabaseCallback callback) {
        if (idProfesores == null || idProfesores.isEmpty()) {
            callback.onFailure("La lista de profesores está vacía");
            return;
        }

        // Primero insertamos la reunión principal
        String urlReunion = BASE_URL + "reunion";

        JSONObject jsonReunion = new JSONObject();
        try {
            // Usamos el primer profesor como responsable principal
            jsonReunion.put("id_profesor", idProfesores.get(0));
            jsonReunion.put("hora_inicio", horaInicio);
            jsonReunion.put("tema", tema);
        } catch (Exception e) {
            callback.onFailure("Error al crear JSON de reunión: " + e.getMessage());
            return;
        }

        RequestBody bodyReunion = RequestBody.create(jsonReunion.toString(), MediaType.parse("application/json"));
        Request requestReunion = new Request.Builder()
                .url(urlReunion)
                .post(bodyReunion)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation") // Para que devuelva la fila insertada
                .build();

        client.newCall(requestReunion).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Error de conexión: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    try {
                        // Obtenemos el ID de la reunión creada
                        JSONArray jsonArray = new JSONArray(responseData);
                        if (jsonArray.length() > 0) {
                            JSONObject reunionInsertada = jsonArray.getJSONObject(0);
                            int idReunion = reunionInsertada.getInt("id_reunion");

                            // Si hay más de un profesor, insertamos los participantes adicionales
                            if (idProfesores.size() > 1) {
                                insertarParticipantesAdicionales(idReunion, idProfesores, callback);
                            } else {
                                callback.onSuccess("Reunión creada correctamente");
                            }
                        } else {
                            callback.onFailure("No se pudo obtener el ID de la reunión creada");
                        }
                    } catch (Exception e) {
                        callback.onFailure("Error al procesar respuesta: " + e.getMessage());
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                    callback.onFailure("Error al crear reunión: " + response.code() + " - " + errorBody);
                }
            }
        });
    }

    // Método para insertar los profesores adicionales como participantes
    private void insertarParticipantesAdicionales(int idReunion, List<Integer> idProfesores, SupabaseCallback callback) {
        // La tabla presumiblemente se llama reunion_participantes
        String url = BASE_URL + "reunion_participantes";

        final int[] completados = {0};
        final boolean[] hayErrores = {false};

        // Saltamos el primer profesor ya que es el profesor principal de la reunión
        for (int i = 1; i < idProfesores.size(); i++) {
            int idProfesor = idProfesores.get(i);

            JSONObject json = new JSONObject();
            try {
                json.put("id_reunion", idReunion);
                json.put("id_profesor", idProfesor);
            } catch (Exception e) {
                callback.onFailure("Error al crear JSON de participante: " + e.getMessage());
                return;
            }

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            final int finalI = i;
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    synchronized (completados) {
                        hayErrores[0] = true;
                        completados[0]++;

                        // Si es el último, notificamos el resultado final
                        if (completados[0] >= idProfesores.size() - 1) {
                            if (hayErrores[0]) {
                                callback.onFailure("Hubo errores al insertar algunos participantes");
                            } else {
                                callback.onSuccess("Reunión y todos los participantes guardados correctamente");
                            }
                        }
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    synchronized (completados) {
                        if (!response.isSuccessful()) {
                            hayErrores[0] = true;
                        }

                        completados[0]++;

                        // Si es el último, notificamos el resultado final
                        if (completados[0] >= idProfesores.size() - 1) {
                            if (hayErrores[0]) {
                                callback.onFailure("Hubo errores al insertar algunos participantes");
                            } else {
                                callback.onSuccess("Reunión y todos los participantes guardados correctamente");
                            }
                        }
                    }
                }
            });
        }

        // Si solo hay un profesor adicional o ninguno, notificamos éxito inmediatamente
        if (idProfesores.size() <= 2) {
            callback.onSuccess("Reunión guardada correctamente");
        }
    }

    // Login verificando en la tabla profesor
    public void loginUser(String email, String password, SupabaseCallback callback) {
        String url = BASE_URL + "profesor" + "?email=eq." + email;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONArray jsonArray = new JSONArray(responseData);
                        if (jsonArray.length() > 0) {
                            JSONObject user = jsonArray.getJSONObject(0);
                            String storedPassword = user.getString("contrasena");

                            if (storedPassword.equals(password)) {
                                callback.onSuccess("Login exitoso");
                            } else {
                                callback.onFailure("Contraseña incorrecta");
                            }
                        } else {
                            callback.onFailure("Usuario no encontrado");
                        }
                    } catch (Exception e) {
                        callback.onFailure("Error procesando datos");
                    }
                } else {
                    callback.onFailure("Error: " + response.code() + " " + response.message());
                }
            }
        });
    }

    public void registerUser(String nombre, String email, String password, SupabaseCallback callback) {
        String url = BASE_URL + "profesor";

        JSONObject json = new JSONObject();
        try {
            json.put("nombre", nombre);
            json.put("email", email);
            json.put("contrasena", password);
        } catch (Exception e) {
            callback.onFailure("Error al crear JSON");
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess("Registro exitoso");
                } else {
                    callback.onFailure("Error: " + response.code() + " " + response.message());
                }
            }
        });
    }
}



