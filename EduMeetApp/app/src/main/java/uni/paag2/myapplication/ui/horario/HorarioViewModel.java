package uni.paag2.myapplication.ui.horario;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class HorarioViewModel extends ViewModel {
    private final MutableLiveData<List<HorarioItem>> horarioList = new MutableLiveData<>();

    public LiveData<List<HorarioItem>> getHorario() {
        return horarioList;
    }

    public void setHorario(List<HorarioItem> lista) {
        horarioList.postValue(lista);
    }
}
