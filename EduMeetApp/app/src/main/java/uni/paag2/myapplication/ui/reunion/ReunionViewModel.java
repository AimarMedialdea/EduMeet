package uni.paag2.myapplication.ui.reunion;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import uni.paag2.myapplication.model.Reunion;

public class ReunionViewModel extends ViewModel {

    // LiveData que contiene la lista de reuniones
    private final MutableLiveData<List<Reunion>> reuniones;

    public ReunionViewModel() {
        reuniones = new MutableLiveData<>();
    }

    // Método para obtener las reuniones
    public LiveData<List<Reunion>> getReuniones() {
        return reuniones;
    }

    // Método para actualizar la lista de reuniones
    public void setReuniones(List<Reunion> nuevasReuniones) {
        reuniones.setValue(nuevasReuniones);
    }
}
