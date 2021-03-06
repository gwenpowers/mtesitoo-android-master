package com.mtesitoo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mtesitoo.adapter.AddProductPagerAdapter;
import com.mtesitoo.backend.model.Product;
import com.mtesitoo.backend.service.ProductRequest;
import com.mtesitoo.backend.service.logic.ICallback;
import com.mtesitoo.backend.service.logic.IProductRequest;
import com.mtesitoo.fragment.AddProductPreviewFragment;
import com.mtesitoo.helper.AddProductHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.mtesitoo.R.id.dots;

public class AddProductActivity extends AppCompatActivity {

    private Context mContext;

    private static final String PREVIEW_FRAGMENT_TAG = "PREVIEW_FRAGMENT";
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.pager)
    ViewPager viewPager;

    @BindView(dots)
    TabLayout pagerIndicator;

    @BindView(R.id.controls_previous)
    TextView prevButton;

    @BindView(R.id.controls_forward)
    TextView nextButton;

    @BindView(R.id.controls_progress)
    ProgressBar progressBar;

    AddProductPagerAdapter pagerAdapter;

    boolean isPreviewShown = false;

    boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        pagerAdapter = new AddProductPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);

        pagerIndicator.setupWithViewPager(viewPager);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                if (position == 0) {
                    prevButton.setVisibility(View.GONE);
                    return;
                }

                prevButton.setVisibility(View.VISIBLE);

                if (position == pagerAdapter.getCount() - 1) {
                    nextButton.setText(getString(R.string.action_preview));
                    nextButton.setCompoundDrawables(null, null, null, null);
                    return;
                }
                nextButton.setText(getString(R.string.action_next));

                nextButton.setCompoundDrawablesWithIntrinsicBounds(null, null, getResources().getDrawable(R.drawable.ic_arrow_next),
                        null);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_product, menu);

        menu.findItem(R.id.action_cancel).setEnabled(!loading);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_cancel) {
            AddProductHelper.getInstance().clearFields();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        if (loading) return;
        //*******@Gwen added 4/14/2019
        // needed to take out super call to onBackPressed in order to use Dialog Builder

     //   super.onBackPressed();


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AddProductHelper.getInstance().clearFields();
                        AddProductActivity.this.finish();


                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }

    @OnClick(R.id.controls_previous)
    void goBack() {
        if (isPreviewShown) {
            isPreviewShown = false;
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_bottom, R.anim.exit_right)
                    .remove(getSupportFragmentManager().findFragmentByTag(PREVIEW_FRAGMENT_TAG)).commit();
            nextButton.setText(getString(R.string.action_preview));

            pagerIndicator.setVisibility(View.VISIBLE);
            return;
        }
        viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
        hideSoftKeyboard();
    }

    @OnClick(R.id.controls_forward)
    void goForward() {

        if (viewPager.getCurrentItem() < pagerAdapter.getCount() - 1) {
            viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
            hideSoftKeyboard();
            return;
        }

        if (!isPreviewShown) {
            isPreviewShown = true;
            pagerIndicator.setVisibility(View.INVISIBLE);
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_bottom, R.anim.exit_right)
                    .add(R.id.add_product_preview_fragment, new AddProductPreviewFragment(), PREVIEW_FRAGMENT_TAG).commit();

            nextButton.setText(getString(R.string.action_submit));
            return;
        }

        submitNewProduct();
    }

    private void hideSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void submitNewProduct() {

        setBottombarLoading(true);

        final Product product = AddProductHelper.getInstance().getProduct();

        if (!product.isCompleted()) {
            Toast.makeText(this, getString(R.string.product_add_incomplete_product), Toast.LENGTH_LONG).show();
            setBottombarLoading(false);
            return;
        }
        Toast.makeText(this, "Submitting new product", Toast.LENGTH_SHORT).show();

        IProductRequest productService = new ProductRequest(this);
        productService.submitProduct(product, new ICallback<String>() {
            @Override
            public void onResult(String result) {
                submitProductPicture(Integer.parseInt(result), product.getmThumbnail(), true);

                // TODO: In some cases the URI List returned is null. So need to check why that is happening.
                if (product.getAuxImages() != null && product.getAuxImages().size() > 0) {
                    for (Uri auxImage : product.getAuxImages()) {
                        submitProductPicture(Integer.parseInt(result), auxImage, false);
                    }
                }
                else {
                    Log.e("submit product error", "onResult: getAuxImages() error");
                }

                Toast.makeText(getApplicationContext(), getString(R.string.product_add_done), Toast.LENGTH_LONG).show();
                AddProductHelper.getInstance().clearFields();
                finish();
            }

            @Override
            public void onError(Exception e) {
                setBottombarLoading(false);
                Log.e("product add error", e.toString());
                Toast.makeText(AddProductActivity.this, getString(R.string.product_add_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitProductPicture(int productId, Uri productPicture, boolean isMainPicture) {
        IProductRequest productService = new ProductRequest(getApplicationContext());
        productService.submitProductPicture(productId, productPicture, isMainPicture, new ICallback<String>() {
            @Override
            public void onResult(String result) {
                Log.d("image thumb upload", "Success");

                Intent intent = new Intent("submit_product_thumbnail");
                intent.putExtra("result", result);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }

            @Override
            public void onError(Exception e) {
                Log.e("image thumb upload err", e.toString());
                Toast.makeText(getApplicationContext(), "Error occurred while uploading Product thumbnail.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @SuppressLint("RestrictedApi")
    private void setBottombarLoading(boolean loading) {
        prevButton.setEnabled(!loading);
        nextButton.setEnabled(!loading);
        this.loading = loading;
        invalidateOptionsMenu();
        if (loading) {
            nextButton.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            nextButton.setVisibility(View.VISIBLE);
        }
    }
}
