package com.develop.vadim.english.Basic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.AnticipateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.develop.vadim.english.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.varunjohn1990.iosdialogs4android.IOSDialog;

import java.util.ArrayList;
import java.util.Random;

import bg.devlabs.transitioner.Transitioner;

public class ChangeWord extends AppCompatActivity {

    private EditText originalWordEditText;
    private EditText translatedWordEditText;

    private MaterialCardView categoriesMaterialCardView;
    private MaterialCardView categoriesMaterialCardViewPlaceHolder;
    private MaterialCardView categoriesMaterialCardViewComeBackPlaceHolder;
    private RecyclerView categoriesRecyclerView;
    private TextView categoriesTextView;

    private BroadcastReceiver updateHasBeenDoneBroadcastReceiver;

    private Word changingWord;
    private ArrayList<String> categories = new ArrayList<>();

    private String category;

    private boolean isCategoryNew;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_word);

        updateHasBeenDoneBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("Close Activity", "Bye!");
                onBackPressed();
            }
        };

        final Handler removingWordHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                Intent intent = new Intent(MainActivity.BROADCAST_ACTION);
                intent.putExtra(getString(R.string.changingWord), changingWord);

                if(isCategoryNew) {
                    intent.putExtra(getString(R.string.addNewCategory), true);
                }

                sendBroadcast(intent);
            }
        };

        changingWord = getIntent().getParcelableExtra(getString(R.string.changeWord));
        categories = getIntent().getStringArrayListExtra(getString(R.string.categoriesToChangeWordActivity));

        category = changingWord.getWordCategory();

        ImageView saveChangesImageView = findViewById(R.id.saveChangesImageView);
        originalWordEditText = findViewById(R.id.editTextRussian);
        translatedWordEditText = findViewById(R.id.editTextEnglish);
        ImageView deleteWordImageView = findViewById(R.id.deleteWordImageView);
        categoriesRecyclerView = findViewById(R.id.categoriesWhileAddingWordRecyclerView);
        categoriesMaterialCardView = findViewById(R.id.categoryChooseCardView);
        categoriesMaterialCardViewPlaceHolder = findViewById(R.id.categoriesMaterialCardView);
        categoriesTextView = findViewById(R.id.addNewWordCategoryTextView);
        categoriesMaterialCardViewComeBackPlaceHolder = findViewById(R.id.categoryChooseCardViewHolder);

        categoriesTextView.setText(changingWord.getWordCategory());

        categoriesMaterialCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                categoriesTextView.setVisibility(View.INVISIBLE);

                Transitioner transitioner = new Transitioner(categoriesMaterialCardView, categoriesMaterialCardViewPlaceHolder);
                transitioner.animateTo(1f, (long) 400, new AccelerateDecelerateInterpolator());
                categoriesMaterialCardView.setCardBackgroundColor(getResources().getColor(R.color.colorWhite));

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        categoriesRecyclerView.setVisibility(View.VISIBLE);
                        categoriesRecyclerView.setAdapter(new CategoriesRecyclerViewAdapter(categories));
                        categoriesRecyclerView.setLayoutManager(new GridLayoutManager(ChangeWord.this, 2));
                    }
                }, 420);
            }
        });

        deleteWordImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new IOSDialog.Builder(v.getContext())
                        .negativeButtonText("Нет")
                        .positiveButtonText("Да")
                        .message(getString(R.string.deleteWordMessage))
                        .negativeClickListener(new IOSDialog.Listener() {
                            @Override
                            public void onClick(IOSDialog iosDialog) {
                                iosDialog.dismiss();
                            }
                        })
                        .positiveClickListener(new IOSDialog.Listener() {
                            @Override
                            public void onClick(IOSDialog iosDialog) {
                                changingWord.removeWordFromService(removingWordHandler);
                                iosDialog.dismiss();

                            }
                        })
                        .build()
                        .show();
            }
        });

        saveChangesImageView.setOnClickListener(new ImageView.OnClickListener() {
            @Override
            public void onClick(View view) {
                category = categoriesTextView.getText().toString();

                saveChanges();
                removingWordHandler.sendMessage(removingWordHandler.obtainMessage());
                onBackPressed();
            }
        });

        originalWordEditText.setText(changingWord.getWordInEnglish());
        translatedWordEditText.setText(changingWord.getWordInRussian());
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(updateHasBeenDoneBroadcastReceiver, new IntentFilter(MainActivity.BROADCAST_UPDATE_HAS_BEEN_DONE_ACTION));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            unregisterReceiver(updateHasBeenDoneBroadcastReceiver);
        }
        catch(IllegalArgumentException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        try {
            unregisterReceiver(updateHasBeenDoneBroadcastReceiver);
        }
        catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void saveChanges() {
        if(!originalWordEditText.getText().toString().equals(changingWord.getWordInEnglish())) {
            MainActivity.reference.child("words").child(changingWord.getInd()).child(Word.englishDatabaseKey).setValue(originalWordEditText.getText().toString()) ;
            changingWord.setWordInEnglish(originalWordEditText.getText().toString());
        }

        if(!translatedWordEditText.getText().toString().equals(changingWord.getWordInEnglish())) {
            MainActivity.reference.child("words").child(changingWord.getInd()).child(Word.russianDatabaseKey).setValue(translatedWordEditText.getText().toString());
            changingWord.setWordInRussian(translatedWordEditText.getText().toString());
        }

        if(!category.equals(changingWord.getWordCategory())) {
            if(categories.contains(categoriesTextView.getText().toString())) {
                MainActivity.reference.child("words").child(changingWord.getInd()).child(Word.categoryDatabaseKey).setValue(categoriesTextView.getText().toString());
            }
            else {
                MainActivity.reference.child("categories").child(String.valueOf(categories.size() - 2)).setValue(category)
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(ChangeWord.this, "Произошла неизвестная ошибка, проверьте поделючение к сети!", Toast.LENGTH_LONG).show();
                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                MainActivity.reference.child("words").child(changingWord.getInd()).child(Word.categoryDatabaseKey).setValue(category);

                            }
                        });

                isCategoryNew = true;
            }

            changingWord.setWordCategory(category);
        }
    }

    private class CategoriesRecyclerViewAdapter extends RecyclerView.Adapter<CategoriesRecyclerViewAdapter.CategoriesRecyclerViewHolder> {

        ArrayList<String> categories;

        private Animation animation = AnimationUtils.loadAnimation(ChangeWord.this, R.anim.appear);

        private int[] materialCardsColors = new int[] {
                R.color.LIGHT_GREEN_TRANSPARENT,
                R.color.LIGHT_PURPLE_TRANSPARENT,
                R.color.CASSANDORA_YELLOW,
                R.color.JADE_DUST_TRANSPARENT,
                R.color.JELLYFISH,
        };


        CategoriesRecyclerViewAdapter(ArrayList<String> categories) {
            this.categories = categories;
        }

        @NonNull
        @Override
        public CategoriesRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.categories_choosing_cell, parent, false);

            return new CategoriesRecyclerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoriesRecyclerViewHolder holder, int position) {
            final int currentPosition = position;

            holder.categoryTextView.setText(categories.get(position));
            holder.materialCardView.setCardBackgroundColor(getResources().getColor(materialCardsColors[new Random().nextInt(materialCardsColors.length)]));

            if(position == getItemCount() - 1) {
                holder.materialCardView.setCardBackgroundColor(getResources().getColor(R.color.DOUBLE_DRAGON_SKIN));
            }

            holder.materialCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Transitioner transitioner = new Transitioner(categoriesMaterialCardView, categoriesMaterialCardViewComeBackPlaceHolder);
                    transitioner.animateTo(1f, (long) 400, new AccelerateDecelerateInterpolator());
                    categoriesMaterialCardView.setCardBackgroundColor(getResources().getColor(R.color.WHITE_TRANSPARENT));

                    categoriesRecyclerView.setVisibility(View.INVISIBLE);

                    categoriesTextView.setText(categories.get(currentPosition));
                    categoriesTextView.setVisibility(View.VISIBLE);

                    if(currentPosition == getItemCount() - 1) {
                        callChooseCategoryDialog();
                    }

                    category = categories.get(currentPosition);
                }
            });

            holder.materialCardView.startAnimation(animation);

        }

        private void callChooseCategoryDialog() {
            final Dialog dialog = new Dialog(ChangeWord.this);
            dialog.setContentView(R.layout.add_new_category_layout);
            final EditText editText = dialog.findViewById(R.id.addNewCategoryEditText);
            final ImageView continueEditText = dialog.findViewById(R.id.addNewCategoryImageView);
            dialog.show();

            continueEditText.setOnClickListener(new ImageView.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!editText.getText().toString().equals("")) {
                        categoriesTextView.setText(editText.getText());
                        category = editText.getText().toString();
                    }

                    dialog.dismiss();
                }

            });
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        class CategoriesRecyclerViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView materialCardView;
            TextView categoryTextView;

            CategoriesRecyclerViewHolder(@NonNull View itemView) {
                super(itemView);

                materialCardView = itemView.findViewById(R.id.categoriesChoosingCellCardView);
                categoryTextView = itemView.findViewById(R.id.categoriesChoosingCellTextView);
            }
        }
    }
}
