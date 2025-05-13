package uni.paag2.myapplication.supabase;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import okhttp3.*;
import uni.paag2.myapplication.model.Reunion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

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


    public interface ReunionesCallback {
        void onSuccess(List<Reunion> reuniones);
        void onFailure(String error);
    }

    public interface SupabaseCallbackAulas {
        void onSuccess(List<String> aulas);
        void onFailure(String error);
    }

    public interface ParticipantesCallback {
        void onSuccess(List<String> participantes);
        void onFailure(String error);
    }

    public void obtenerParticipantesPorReunion(int idReunion, ParticipantesCallback callback) {
        new Thread(() -> {
            try {
                Log.d("SupabaseHelper", "Obteniendo participantes para reunión ID: " + idReunion);

                String url = BASE_URL + "profesor_reunion?id_reunion=eq." + idReunion + "&unirme=eq.true&select=id_profesor(nombre)";
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", API_KEY)
                        .addHeader("Authorization", "Bearer " + API_KEY)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseBody);
                    List<String> participantes = new ArrayList<>();

                    Log.d("SupabaseHelper", "Respuesta recibida con " + jsonArray.length() + " participantes");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        JSONObject profesor = obj.getJSONObject("id_profesor");
                        String nombre = profesor.getString("nombre");
                        participantes.add(nombre);
                        Log.d("SupabaseHelper", "Participante añadido: " + nombre);
                    }

                    callback.onSuccess(participantes);
                } else {
                    String errorMsg = "HTTP Error: " + response.code();
                    Log.e("SupabaseHelper", errorMsg);
                    callback.onFailure(errorMsg);
                }
            } catch (Exception e) {
                String errorMsg = "Excepción al obtener participantes: " + e.getMessage();
                Log.e("SupabaseHelper", errorMsg, e);
                callback.onFailure(errorMsg);
            }
        }).start();
    }



    // Método para obtener todos los profesores
    public void obtenerDatosProfesorPorId(int idProfesor, SupabaseCallback callback) {
        // URL corregida con la sintaxis correcta de filtro en Supabase
        String url = BASE_URL + "profesor?select=id_profesor,nombre,email&id_profesor=eq." + idProfesor;

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
                    String errorBody = response.body() != null ? response.body().string() : "No error details";
                    callback.onFailure("Error: " + response.code() + " " + response.message() + "\n" + errorBody);
                }
            }
        });
    }


    // Método para insertar una reunión con múltiples participantes
    public void insertarReunion(String tema, String fecha, String hora, String sala, int idProfesor, SupabaseCallback callback) {
        boolean finished = false;
        String url = BASE_URL + "reunion";

        JSONObject json = new JSONObject();
        try {
            json.put("tema", tema);
            json.put("fecha", fecha);               // Formato YYYY-MM-DD
            json.put("hora_inicio", hora);          // Formato HH:mm:ss
            json.put("sala", sala);
            json.put("id_profesor", idProfesor);    // ID del profesor desde SharedPreferences
        } catch (Exception e) {
            callback.onFailure("Error al crear JSON de reunión: " + e.getMessage());
            finished = true;
        }
        if (!finished) {
            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure("Error de conexión: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        callback.onSuccess("Reunión insertada correctamente");
                    } else {
                        callback.onFailure("Error al insertar reunión: " + response.code() + " - " + responseData);
                    }
                }
            });
        }

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
    public void loginUser(String email, String password, Context context, SupabaseCallback callback) {
        String url = BASE_URL + "profesor?email=eq." + email;

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
                                // ✅ Guardamos el ID del profesor
                                int idProfesor = user.getInt("id_profesor");
                                SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putInt("id_profesor", idProfesor);
                                editor.apply();

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


    public void registerUser(String nombre, String email, String password, int idDepartamento, SupabaseCallback callback) {
        String url = BASE_URL + "profesor";

        JSONObject json = new JSONObject();
        try {
            json.put("nombre", nombre);
            json.put("email", email);
            json.put("contrasena", password);
            json.put("id_departamento", idDepartamento);
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


    public void obtenerDepartamentos(SupabaseCallback callback) {
        String url = BASE_URL + "departamento?select=id_departamento,nombre";

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
                    callback.onSuccess(response.body().string());
                } else {
                    callback.onFailure("Error al obtener departamentos");
                }
            }
        });
    }

    public void obtenerAsignaturas(SupabaseCallback callback) {
        String url = BASE_URL + "asignatura?select=id_asignatura,nombre";
        getRequest(url, callback);
    }

    public void insertarHorario(int idProfesor, int idAsignatura, String horaInicio, String horaFin, String dia, SupabaseCallback callback) {
        String url = BASE_URL + "profesor_asignatura";

        JSONObject json = new JSONObject();
        try {
            json.put("id_profesor", idProfesor);
            json.put("id_asignatura", idAsignatura);
            json.put("hora_inicio", horaInicio);
            json.put("hora_fin", horaFin);
            json.put("dia", dia);
        } catch (Exception e) {
            callback.onFailure("Error creando JSON");
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
                    callback.onSuccess("Horario insertado");
                } else {
                    callback.onFailure("Error: " + response.message());
                }
            }
        });
    }

    public void obtenerHorarioProfesor(int id_profesor, SupabaseCallback callback) {
        // Asegúrate de que la consulta incluya el campo id de la relación
        String url = BASE_URL + "profesor_asignatura?id_profesor=eq." + id_profesor + "&select=*,asignatura(*)";

        Log.d("SupabaseHelper", "Obteniendo horario para profesor: " + id_profesor + " con URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SupabaseHelper", "Error de red al obtener horario: " + e.getMessage());
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d("SupabaseHelper", "Respuesta de obtenerHorarioProfesor: " + responseBody);

                if (response.isSuccessful()) {
                    callback.onSuccess(responseBody);
                } else {
                    Log.e("SupabaseHelper", "Error al obtener horario: " + response.code() + " " + response.message());
                    callback.onFailure("Error: " + response.code() + " " + response.message() + " - " + responseBody);
                }
            }
        });
    }




    private void getRequest(String url, SupabaseCallback callback) {
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
                    callback.onSuccess(response.body().string());
                } else {
                    callback.onFailure("Error de red");
                }
            }
        });
    }

    public void eliminarHorario(int id_relacion, SupabaseCallback callback) {
        if (id_relacion <= 0) {
            Log.e("SupabaseHelper", "Error: Intentando eliminar con ID inválido: " + id_relacion);
            callback.onFailure("ID de relación inválido: " + id_relacion);
            return;
        }

        // Prueba primero con id_relacion
        String url = BASE_URL + "profesor_asignatura?id_relacion=eq." + id_relacion;
        Log.d("SupabaseHelper", "Intentando eliminar con URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SupabaseHelper", "Error de red al eliminar horario: " + e.getMessage());
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d("SupabaseHelper", "Respuesta al eliminar: Código " + response.code() + ", Cuerpo: " + responseBody);

                if (response.isSuccessful()) {
                    callback.onSuccess("Horario eliminado correctamente");
                } else {
                    // Si falla con id_relacion, intentar con id
                    if (response.code() == 400 || response.code() == 404) {
                        Log.d("SupabaseHelper", "Primer intento falló, probando con campo 'id'");
                        eliminarConId(id_relacion, callback);
                    } else {
                        Log.e("SupabaseHelper", "Error al eliminar horario: " + response.code() + " " + response.message());
                        callback.onFailure("Error: " + response.code() + " " + response.message() + " - " + responseBody);
                    }
                }
            }
        });
    }

    private void eliminarConId(int id, SupabaseCallback callback) {
        String url = BASE_URL + "profesor_asignatura?id=eq." + id;
        Log.d("SupabaseHelper", "Intentando eliminar con URL alternativa: " + url);

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SupabaseHelper", "Error de red al eliminar horario (2do intento): " + e.getMessage());
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d("SupabaseHelper", "Respuesta al eliminar (2do intento): Código " + response.code() + ", Cuerpo: " + responseBody);

                if (response.isSuccessful()) {
                    callback.onSuccess("Horario eliminado correctamente");
                } else {
                    Log.e("SupabaseHelper", "Error al eliminar horario (2do intento): " + response.code() + " " + response.message());
                    callback.onFailure("Error: " + response.code() + " " + response.message() + " - " + responseBody);
                }
            }
        });
    }

    public void obtenerReunionesPorProfesor(int idProfesor, ReunionesCallback callback) {
        String url = BASE_URL + "reunion?id_profesor=eq." + idProfesor;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Error de conexión: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) {
                    callback.onFailure("Respuesta vacía del servidor");
                    return;
                }

                String responseData = response.body().string();
                if (response.isSuccessful()) {
                    try {
                        JSONArray jsonArray = new JSONArray(responseData);
                        List<Reunion> reuniones = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            Reunion reunion = new Reunion();
                            reunion.setIdReunion(obj.getInt("id_reunion"));
                            reunion.setTema(obj.getString("tema"));
                            reunion.setFecha(obj.optString("fecha", ""));
                            reunion.setHoraInicio(obj.optString("hora_inicio", ""));
                            reunion.setSala(obj.optString("sala", ""));
                            reunion.setIdProfesor(obj.optInt("id_profesor", -1));

                            reuniones.add(reunion);  // Aquí se añade la reunión a la lista
                        }

                        // Llamar al callback.onSuccess con la lista de reuniones
                        callback.onSuccess(reuniones);

                    } catch (JSONException e) {
                        callback.onFailure("Error al parsear JSON: " + e.getMessage());
                    }
                } else {
                    callback.onFailure("Error HTTP: " + response.code());
                }
            }
        });
    }


    public void actualizarReunion(int idReunion, String tema, String fecha, String hora, String sala, int idProfesor, SupabaseCallback callback) {
        JSONObject reunionJson = new JSONObject();
        try {
            reunionJson.put("tema", tema);
            reunionJson.put("fecha", fecha);
            reunionJson.put("hora_inicio", hora);
            reunionJson.put("sala", sala);
            reunionJson.put("id_profesor", idProfesor);
        } catch (JSONException e) {
            callback.onFailure("Error al crear JSON: " + e.getMessage());
            return;
        }

        String url = BASE_URL + "/reunion?id_reunion=eq." + idReunion;
        Request request = new Request.Builder()
                .url(url)
                .patch(RequestBody.create(reunionJson.toString(), MediaType.parse("application/json")))
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
                    callback.onSuccess(response.body().string());
                } else {
                    callback.onFailure("Error " + response.code() + ": " + response.body().string());
                }
            }
        });
    }

    public void obtenerTodasLasReuniones(SupabaseCallback callback) {
        String url = BASE_URL + "reunion?select=id_reunion,tema,fecha,hora_inicio,sala,id_profesor";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().string());
                } else {
                    callback.onFailure("Código: " + response.code());
                }
            }
        });
    }

    public void obtenerDatosProfesorPorEmail(String email, SupabaseCallback callback) {
        // URL para buscar por email
        String url = BASE_URL + "profesor?select=id_profesor,nombre,email&email=eq." + email;

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
                Log.e("SUPABASE", "Error en la solicitud: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Log.d("SUPABASE", "Respuesta exitosa: " + responseData);
                    callback.onSuccess(responseData);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No error details";
                    Log.e("SUPABASE", "Error en la respuesta: " + response.code() + " " + errorBody);
                    callback.onFailure("Error: " + response.code() + " " + response.message() + "\n" + errorBody);
                }
            }
        });
    }

    public void obtenerIdProfesorPorEmail(String email, Context context, SupabaseCallback callback) {
        OkHttpClient client = new OkHttpClient();
        String url = BASE_URL + "/rest/v1/profesor?correo=eq." + email;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Error de red al obtener el ID del profesor");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    try {
                        JSONArray jsonArray = new JSONArray(body);
                        if (jsonArray.length() > 0) {
                            JSONObject profesor = jsonArray.getJSONObject(0);
                            int idProfesor = profesor.getInt("id_profesor");
                            callback.onSuccess(String.valueOf(idProfesor));
                        } else {
                            callback.onFailure("Profesor no encontrado");
                        }
                    } catch (JSONException e) {
                        callback.onFailure("Error al parsear JSON: " + e.getMessage());
                    }
                } else {
                    callback.onFailure("Error al consultar profesor: " + response.code());
                }
            }
        });
    }
    public void obtenerAulas(SupabaseCallbackAulas callback) {
        new Thread(() -> {
            try {
                String url = BASE_URL + "aula?select=descripcion";

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", API_KEY)
                        .addHeader("Authorization", "Bearer " + API_KEY)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseBody);
                    List<String> aulas = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        aulas.add(obj.getString("descripcion"));
                    }

                    callback.onSuccess(aulas);
                } else {
                    callback.onFailure("Error HTTP: " + response.code());
                }

            } catch (Exception e) {
                callback.onFailure("Excepción: " + e.getMessage());
            }
        }).start();
    }

    public void obtenerReunionesUnidasPorProfesor(int idProfesor, ReunionesCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Reuniones a las que se ha unido (unirme = true)
                String url = BASE_URL + "profesor_reunion?id_profesor=eq." + idProfesor
                        + "&unirme=eq.true&select=reunion(*)";

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", API_KEY)
                        .addHeader("Authorization", "Bearer " + API_KEY)
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    callback.onFailure("Error al obtener reuniones unidas: " + response.message());
                    return;
                }

                String responseBody = response.body().string();
                JSONArray jsonArray = new JSONArray(responseBody);
                List<Reunion> reuniones = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject reunionWrapper = jsonArray.getJSONObject(i);
                    JSONObject jsonReunion = reunionWrapper.getJSONObject("reunion");

                    Reunion reunion = new Reunion();
                    // Corregir aquí: estabas asignando id_profesor a idReunion
                    reunion.setIdReunion(jsonReunion.getInt("id_reunion"));
                    reunion.setTema(jsonReunion.getString("tema"));
                    reunion.setFecha(jsonReunion.getString("fecha"));
                    reunion.setHoraInicio(jsonReunion.getString("hora_inicio"));
                    reunion.setSala(jsonReunion.getString("sala"));

                    // Asignar el id_profesor si está disponible en la respuesta
                    if (jsonReunion.has("id_profesor")) {
                        reunion.setIdProfesor(jsonReunion.getInt("id_profesor"));
                    }

                    reuniones.add(reunion);

                    // Log para depuración
                    Log.d("SupabaseHelper", "Añadida reunión: ID=" + reunion.getIdReunion() +
                            ", Tema=" + reunion.getTema());
                }

                // Log para depuración
                Log.d("SupabaseHelper", "Total reuniones unidas cargadas: " + reuniones.size());

                callback.onSuccess(reuniones);

            } catch (Exception e) {
                Log.e("SupabaseHelper", "Error en obtenerReunionesUnidasPorProfesor: " + e.getMessage(), e);
                callback.onFailure("Excepción: " + e.getMessage());
            }
        });
    }
    public void obtenerTodasLasReunionesCard(SupabaseCallback callback) {
        String url = BASE_URL + "/rest/v1/reunion?select=*";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
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
                    callback.onSuccess(responseData);
                } else {
                    callback.onFailure("Error: " + response.code() + " " + response.message());
                }
            }
        });
    }

    public void obtenerAsignaturasCard(SupabaseCallback callback) {
        String url = BASE_URL + "/rest/v1/asignatura?select=*";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
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
                    callback.onSuccess(responseData);
                } else {
                    callback.onFailure("Error: " + response.code() + " " + response.message());
                }
            }
        });
    }

    public void obtenerHorarioProfesor(int idProfesor, String diaSemana, SupabaseCallback callback) {
        String url = BASE_URL + "/rest/v1/profesor_asignatura?id_profesor=eq." + idProfesor +
                "&dia=eq." + diaSemana.toLowerCase();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
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
                    callback.onSuccess(responseData);
                } else {
                    callback.onFailure("Error: " + response.code() + " " + response.message());
                }
            }
        });
    }

    public String getSUPABASE_URL() {
        return BASE_URL;
    }

    public String getSUPABASE_KEY() {
        return API_KEY;
    }

    public OkHttpClient getClient() {
        return client;
    }
}






