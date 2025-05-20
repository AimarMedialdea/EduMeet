package uni.paag2.myapplication.ui.reunion;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HorarioManager {
    private final String SUPABASE_URL;
    private final String SUPABASE_KEY;
    private final OkHttpClient client;
    private final Context context;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("es", "ES"));

    private final String[] BLOQUES_HORARIOS = {
            "08:00:00", "08:30:00", "09:00:00", "09:30:00", "10:00:00", "10:30:00",
            "11:00:00", "11:30:00", "12:00:00", "12:30:00", "13:00:00", "13:30:00",
            "14:00:00", "14:30:00", "15:00:00", "15:30:00", "16:00:00"
    };

    public HorarioManager(Context context, String supabaseUrl, String supabaseKey) {
        this.context = context;
        this.SUPABASE_URL = supabaseUrl;
        this.SUPABASE_KEY = supabaseKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public interface HorarioCallback {
        void onSuccess(String nuevoHorario);
        void onFailure(String error);
    }

    public interface SupabaseCallbackList {
        void onSuccess(List<Integer> result);
        void onFailure(String error);
    }

    public void buscarHorarioDisponible(int idReunion, HorarioCallback callback) {
        obtenerReunion(idReunion, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Error al obtener información de la reunión: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        if (jsonArray.length() == 0) {
                            callback.onFailure("No se encontró la reunión");
                            return;
                        }
                        JSONObject reunionData = jsonArray.getJSONObject(0);
                        String fecha = reunionData.getString("fecha");
                        String diaOriginal = obtenerDiaSemana(fecha);

                        // En vez de obtener participantes, obtenemos todas las IDs de profesores
                        obtenerTodasLasIdsProfesores(new SupabaseCallbackList() {
                            @Override
                            public void onSuccess(List<Integer> idsProfesores) {
                                System.out.println("Profesores para buscar horarios comunes: " + idsProfesores);

                                buscarHorariosComunes(idsProfesores, diaOriginal, new HorarioCallback() {
                                    @Override
                                    public void onSuccess(String nuevoHorario) {
                                        if (nuevoHorario != null) {
                                            actualizarHoraReunion(idReunion, nuevoHorario, callback);
                                        } else {
                                            callback.onFailure("No se encontraron horarios disponibles para todos los profesores");
                                        }
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        callback.onFailure(error);
                                    }
                                });
                            }

                            @Override
                            public void onFailure(String error) {
                                callback.onFailure("Error al obtener IDs de profesores: " + error);
                            }
                        });

                    } catch (JSONException e) {
                        callback.onFailure("Error al procesar datos de la reunión: " + e.getMessage());
                    }
                } else {
                    callback.onFailure("Error al obtener reunión: " + response.code());
                }
            }
        });
    }

    public void obtenerTodasLasIdsProfesores(SupabaseCallbackList callback) {
        String url = SUPABASE_URL + "/rest/v1/profesor";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Error de red al obtener IDs de profesores");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    try {
                        JSONArray jsonArray = new JSONArray(body);
                        List<Integer> idsProfesores = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject profesor = jsonArray.getJSONObject(i);
                            idsProfesores.add(profesor.getInt("id_profesor"));
                        }
                        callback.onSuccess(idsProfesores);
                    } catch (JSONException e) {
                        callback.onFailure("Error al parsear JSON: " + e.getMessage());
                    }
                } else {
                    callback.onFailure("Error al consultar profesores: " + response.code());
                }
            }
        });
    }


    private void obtenerReunion(int idReunion, Callback callback) {
        String url = SUPABASE_URL + "/rest/v1/reunion?id_reunion=eq." + idReunion;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .build();
        client.newCall(request).enqueue(callback);
    }

    private void obtenerParticipantesReunion(int idReunion, Callback callback) {
        String url = SUPABASE_URL + "/rest/v1/profesor_reunion?id_reunion=eq." + idReunion + "&unirme=eq.true&select=id_profesor";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .build();
        client.newCall(request).enqueue(callback);
    }

    private void buscarHorariosComunes(List<Integer> idsProfesores, String diaSemana, HorarioCallback callback) {
        Map<String, Integer> horariosDisponibles = new HashMap<>();
        CountDownLatch latch = new CountDownLatch(idsProfesores.size());
        AtomicReference<String> error = new AtomicReference<>(null);

        System.out.println("Profesores para buscar horarios comunes: " + idsProfesores);

        for (Integer idProfesor : idsProfesores) {
            obtenerHorarioProfesor(idProfesor, diaSemana, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    String mensajeError = "Error al obtener horario del profesor " + idProfesor + ": " + e.getMessage();
                    System.out.println(mensajeError);
                    error.set(mensajeError);
                    latch.countDown();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseData = response.body().string();
                            JSONArray horarioArray = new JSONArray(responseData);
                            Set<String> horasOcupadas = new HashSet<>();

                            for (int i = 0; i < horarioArray.length(); i++) {
                                JSONObject clase = horarioArray.getJSONObject(i);
                                if (clase.has("id_asignatura")) {
                                    int idAsignatura = clase.getInt("id_asignatura");
                                    String asignatura = obtenerNombreAsignaturaSincrono(idAsignatura);
                                    if (!asignatura.equalsIgnoreCase("Zaintza")) {
                                        horasOcupadas.add(clase.getString("hora_inicio") + "-" + clase.getString("hora_fin"));
                                    }
                                } else {
                                    horasOcupadas.add(clase.getString("hora_inicio") + "-" + clase.getString("hora_fin"));
                                }
                            }

                            synchronized (horariosDisponibles) {
                                for (String bloque : BLOQUES_HORARIOS) {
                                    boolean ocupado = false;
                                    for (String ocupada : horasOcupadas) {
                                        String[] horas = ocupada.split("-");
                                        if (estaEntre(bloque, horas[0], horas[1])) {
                                            ocupado = true;
                                            break;
                                        }
                                    }
                                    if (!ocupado) {
                                        horariosDisponibles.put(bloque, horariosDisponibles.getOrDefault(bloque, 0) + 1);
                                        System.out.println("Profesor " + idProfesor + " tiene bloque libre: " + bloque);
                                    }
                                }
                            }

                        } catch (JSONException e) {
                            String mensajeError = "Error al procesar horario del profesor " + idProfesor + ": " + e.getMessage();
                            System.out.println(mensajeError);
                            error.set(mensajeError);
                        }
                    } else {
                        String mensajeError = "Error al obtener horario del profesor " + idProfesor + ": " + response.code();
                        System.out.println(mensajeError);
                        error.set(mensajeError);
                    }
                    latch.countDown();
                }
            });
        }

        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                callback.onFailure("Tiempo de espera agotado al obtener horarios");
                return;
            }
        } catch (InterruptedException e) {
            callback.onFailure("Interrupción al esperar horarios: " + e.getMessage());
            return;
        }

        if (error.get() != null) {
            callback.onFailure(error.get());
            return;
        }

        TreeMap<String, Integer> horariosOrdenados = new TreeMap<>(horariosDisponibles);
        int total = idsProfesores.size();
        for (Map.Entry<String, Integer> entry : horariosOrdenados.entrySet()) {
            if (entry.getValue() == total) {
                callback.onSuccess(entry.getKey());
                return;
            }
        }

        callback.onSuccess(null);
    }



    private void obtenerHorarioProfesor(int idProfesor, String diaSemana, Callback callback) {
        String url = SUPABASE_URL + "/rest/v1/profesor_asignatura?id_profesor=eq." + idProfesor +
                "&dia=eq." + diaSemana.toLowerCase() +
                "&select=hora_inicio,hora_fin,id_asignatura";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .build();
        client.newCall(request).enqueue(callback);
    }

    private String obtenerNombreAsignaturaSincrono(int idAsignatura) {
        String url = SUPABASE_URL + "/rest/v1/asignatura?id_asignatura=eq." + idAsignatura + "&select=nombre";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .build();

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> result = new AtomicReference<>("");
        final AtomicReference<IOException> exception = new AtomicReference<>(null);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                exception.set(e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String data = response.body().string();
                        JSONArray array = new JSONArray(data);
                        if (array.length() > 0) {
                            result.set(array.getJSONObject(0).getString("nombre"));
                        }
                    } catch (JSONException e) {
                        result.set("");
                    }
                }
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return "";
        }

        return result.get() != null ? result.get() : "";
    }

    private boolean estaEntre(String hora, String horaInicio, String horaFin) {
        try {
            Date time = timeFormat.parse(hora);
            Date inicio = timeFormat.parse(horaInicio);
            Date fin = timeFormat.parse(horaFin);
            return !time.before(inicio) && time.before(fin);
        } catch (ParseException e) {
            return false;
        }
    }

    private void actualizarHoraReunion(int idReunion, String nuevaHora, HorarioCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("hora_inicio", nuevaHora);
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    jsonObject.toString()
            );
            String url = SUPABASE_URL + "/rest/v1/reunion?id_reunion=eq." + idReunion;
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .patch(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure("Error al actualizar hora de reunión: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        callback.onSuccess(nuevaHora);
                    } else {
                        callback.onFailure("Error al actualizar hora de reunión: " + response.code() + " - " + response.body().string());
                    }
                }
            });
        } catch (Exception e) {
            callback.onFailure("Error al actualizar hora de reunión: " + e.getMessage());
        }
    }

    private String obtenerDiaSemana(String fecha) {
        try {
            SimpleDateFormat formatoFecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = formatoFecha.parse(fecha);
            return eliminarTildes(new SimpleDateFormat("EEEE", new Locale("es", "ES")).format(date).toLowerCase());
        } catch (ParseException e) {
            return "";
        }
    }

    private String eliminarTildes(String input) {
        return input.replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U");
    }

    private int obtenerIdProfesorActual() {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        return prefs.getInt("id_profesor", -1);
    }
}
