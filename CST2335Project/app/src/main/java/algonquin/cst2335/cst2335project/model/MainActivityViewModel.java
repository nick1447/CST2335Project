package algonquin.cst2335.cst2335project.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

public class MainActivityViewModel extends ViewModel {
    public MutableLiveData<ArrayList<Movie>> movies = new MutableLiveData<>();
    public MutableLiveData<Movie> selectedMovie = new MutableLiveData<>();
}
