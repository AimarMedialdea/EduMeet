package uni.paag2.myapplication.ui.horario;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class HorarioViewModel extends ViewModel {
    private final MutableLiveData<List<String>> horarioList = new MutableLiveData<>();

    public LiveData<List<String>> getHorario() {
        return horarioList;
    }

    public void setHorario(List<String> lista) {
        horarioList.postValue(lista);
    }
}
