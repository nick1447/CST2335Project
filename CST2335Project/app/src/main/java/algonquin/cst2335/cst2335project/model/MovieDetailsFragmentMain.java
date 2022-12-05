package algonquin.cst2335.cst2335project.model;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import algonquin.cst2335.cst2335project.R;
import algonquin.cst2335.cst2335project.database.MovieDatabase;
import algonquin.cst2335.cst2335project.databinding.MovieDetailsViewBinding;
import algonquin.cst2335.cst2335project.database.MovieDAO;


public class MovieDetailsFragmentMain extends Fragment {
    private static final String baseUrl = "http://www.omdbapi.com/?apikey=6c9862c2&t=";
    private MovieDetailsViewBinding binding;
    private Movie selectedMovie;
    private MovieDAO movieDAO;
    private MovieDatabase database;
    private RecyclerView.Adapter adapter;
    private ArrayList<Movie> movies;

    public MovieDetailsFragmentMain(Movie selectedMovie, RecyclerView.Adapter adapter, ArrayList<Movie> movies) {
        this.selectedMovie = selectedMovie;
        this.adapter = adapter;
        this.movies = movies;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        binding = MovieDetailsViewBinding.inflate(inflater);

        //Set class variables for database access
        database = Room.databaseBuilder(getContext(), MovieDatabase.class, "Movies")
                .build();
        movieDAO = database.movieDAO();

        binding.fragMovieAddFavouriteButton.setText("Remove");
        binding.fragMovieAddFavouriteButton.setOnClickListener((click) -> {

            //AlertDialog
            new AlertDialog.Builder(getContext())
                .setTitle("Warning!")
                .setMessage(
                    "Are you sure you want to remove " +
                    selectedMovie.getName() +
                    " from your favourites?"
                )
                .setPositiveButton("Yes", (dialog, ck) -> {
                    //Remove movie from local database
                    Executor databaseThread = Executors.newSingleThreadExecutor();
                    databaseThread.execute(() -> {
                        movieDAO.deleteMovie(selectedMovie);
                        getActivity().runOnUiThread(() -> {
                            movies.remove(selectedMovie);
                            adapter.notifyDataSetChanged();
                        });
                    });

                    getActivity().runOnUiThread(() -> {
                        Snackbar.make(
                                getActivity().getWindow().getDecorView().findViewById(R.id.mainMovieRecycler),
                                "You deleted " + selectedMovie.getName(),
                                Snackbar.LENGTH_LONG
                            )
                            .setAction("Undo", (undoClick) -> {
                                Executor undoDatabaseThread = Executors.newSingleThreadExecutor();
                                undoDatabaseThread.execute(() -> {
                                    movieDAO.insertMovie(selectedMovie);
                                    getActivity().runOnUiThread(() -> {
                                        movies.add(selectedMovie);
                                        adapter.notifyDataSetChanged();
                                    });
                                });
                            })
                            .show();
                    });
                })
                .setNegativeButton("No", (dialog, ck) -> {})
                .create()
                .show();

            //Shutdown fragment
            getActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .remove(this)
                    .commit();
        });

        binding.fragMovieTitle.setText(selectedMovie.getName());
        binding.fragMovieRuntime.setText(selectedMovie.getRunTime());
        binding.fragMovieYear.setText(selectedMovie.getYear());
        binding.fragMoviePlot.setText(selectedMovie.getPlot());
        binding.fragMovieActors.setText(selectedMovie.getMainActors());
        binding.fragMovieRated.setText(selectedMovie.getRating());

        return binding.getRoot();
    }
}
