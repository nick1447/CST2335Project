package algonquin.cst2335.cst2335project.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import algonquin.cst2335.cst2335project.R;
import algonquin.cst2335.cst2335project.databinding.ActivitySearchBinding;
import algonquin.cst2335.cst2335project.databinding.MovieViewBinding;
import algonquin.cst2335.cst2335project.model.Movie;
import algonquin.cst2335.cst2335project.model.MovieBuilder;
import algonquin.cst2335.cst2335project.model.MovieDetailsFragment;
import algonquin.cst2335.cst2335project.model.SearchActivityViewModel;


public class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;
    private RecyclerView.Adapter<SearchMovieRow> adapter;
    private ArrayList<Movie> movies = new ArrayList<>();
    private RequestQueue requestQueue;
    private SearchActivityViewModel viewModel;
    private static final String baseURL = "http://www.omdbapi.com/?apikey=6c9862c2";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SearchActivityViewModel.class);
        movies = viewModel.movies.getValue();

        SharedPreferences savedMoviePrefs = getSharedPreferences("myData", Context.MODE_PRIVATE);
        binding.movieSearch.setText(savedMoviePrefs.getString("movieName", ""));

        requestQueue = Volley.newRequestQueue(this);

        viewModel.selectedMovie.observe(this, (newMovieValue) -> {
            MovieDetailsFragment movieDetailsFragment = new MovieDetailsFragment(newMovieValue);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragmentFrame, movieDetailsFragment)
                    .addToBackStack("")
                    .commit();
        });

        binding.movieRecycler.setAdapter(adapter = new RecyclerView.Adapter<SearchMovieRow>() {
            @NonNull
            @Override
            public SearchMovieRow onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                MovieViewBinding binding = MovieViewBinding.inflate(getLayoutInflater());
                return new SearchMovieRow(binding.getRoot());
            }

            @Override
            public void onBindViewHolder(@NonNull SearchMovieRow holder, int position) {
                //Set movie poster
                ImageRequest imageRequest = new ImageRequest(
                        movies.get(position).getCoverURL(),
                        (Bitmap response) -> {
                            holder.movieImage.setImageBitmap(response);
                        },
                        200, 400, ImageView.ScaleType.CENTER, null,
                        error -> {
                            System.err.println("ERROR GETTING IMAGE");
                        }
                );
                requestQueue.add(imageRequest);

                //set TextView values
                holder.movieName.setText(movies.get(position).getName());
                holder.movieYear.setText(movies.get(position).getYear());
                holder.movieType.setText(movies.get(position).getType());
            }

            @Override
            public int getItemCount() {
                return movies.size();
            }
        });

        binding.movieRecycler.setLayoutManager(new GridLayoutManager(this, 1));

        binding.searchButton.setOnClickListener(click -> {
            movies = new ArrayList<>();
            adapter.notifyDataSetChanged();

            SharedPreferences.Editor editor = savedMoviePrefs.edit();
            editor.putString("movieName", binding.movieSearch.getText().toString());
            editor.apply();

            InputMethodManager keyboardManager = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
            keyboardManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);

            String url;
            int pageNumber = 1;
            try {
                url = baseURL +
                        "&s=" + URLEncoder.encode(binding.movieSearch.getText().toString(), "UTF-8") +
                        "&p=" + pageNumber;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                url = "";
            }

            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    (response) -> {
                        try {
                            JSONArray results = response.getJSONArray("Search");
                            for(int movieIndex = 0; movieIndex < results.length(); movieIndex++) {
                                JSONObject jsonMovie = (JSONObject) results.get(movieIndex);
                                Movie movie = MovieBuilder.create()
                                        .setYear(jsonMovie.getString("Year"))
                                        .setCoverURL(jsonMovie.getString("Poster"))
                                        .setType(jsonMovie.getString("Type"))
                                        .setName(jsonMovie.getString("Title"))
                                        .build();

                                movies.add(movie);
                                adapter.notifyItemInserted(movies.size() - 1);
                            }


                        } catch(JSONException e) {
                            e.printStackTrace();
                            System.err.println("JSON ERROR");
                        }
                    },
                    (error) -> {
                        movies.add(
                            MovieBuilder.create()
                                    .setName("ERROR")
                                    .build()
                        );
                        adapter.notifyItemInserted(movies.size() - 1);
                    });
            requestQueue.add(jsonRequest);

        });

        if(movies == null) {
            viewModel.movies.postValue(movies = new ArrayList<Movie>());
        }

    }

    public class SearchMovieRow extends RecyclerView.ViewHolder {

        ImageView movieImage;
        TextView movieName;
        TextView movieYear;
        TextView movieType;

        public SearchMovieRow(@NonNull View itemView) {
            super(itemView);

            movieImage = itemView.findViewById(R.id.movieImage);
            movieName = itemView.findViewById(R.id.movieName);
            movieYear = itemView.findViewById(R.id.movieYear);
            movieType = itemView.findViewById(R.id.movieType);

            itemView.setOnClickListener(click -> {
                int position = getAbsoluteAdapterPosition();
                Movie selectedMovie = movies.get(position);

                viewModel.selectedMovie.postValue(selectedMovie);
            });
        }
    }
}