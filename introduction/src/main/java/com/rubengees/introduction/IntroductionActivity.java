package com.rubengees.introduction;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.rubengees.introduction.adapter.PagerAdapter;
import com.rubengees.introduction.common.DotIndicatorManager;
import com.rubengees.introduction.interfaces.IndicatorManager;
import com.rubengees.introduction.style.Style;
import com.rubengees.introduction.util.ButtonManager;
import com.rubengees.introduction.util.OrientationUtils;
import com.rubengees.introduction.view.SmoothViewPager;

import java.util.ArrayList;
import java.util.Collections;

import static com.rubengees.introduction.IntroductionBuilder.BUNDLE_ALLOW_BACK_PRESS;
import static com.rubengees.introduction.IntroductionBuilder.BUNDLE_ORIENTATION;
import static com.rubengees.introduction.IntroductionBuilder.BUNDLE_SHOW_INDICATOR;
import static com.rubengees.introduction.IntroductionBuilder.BUNDLE_SHOW_PREVIOUS_BUTTON;
import static com.rubengees.introduction.IntroductionBuilder.BUNDLE_SKIP_RESOURCE;
import static com.rubengees.introduction.IntroductionBuilder.BUNDLE_SKIP_STRING;
import static com.rubengees.introduction.IntroductionBuilder.BUNDLE_SLIDES;
import static com.rubengees.introduction.IntroductionBuilder.BUNDLE_STYLE;
import static com.rubengees.introduction.IntroductionBuilder.ORIENTATION_BOTH;
import static com.rubengees.introduction.IntroductionBuilder.ORIENTATION_LANDSCAPE;
import static com.rubengees.introduction.IntroductionBuilder.ORIENTATION_PORTRAIT;

/**
 * The Activity which shows the introduction finally to the user.
 *
 * @author Ruben Gees
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class IntroductionActivity extends AppCompatActivity {

    public static final String OPTION_RESULT = "introduction_option_result";
    public static final String STATE_PREVIOUS_PAGER_POSITION = "previous_pager_position";

    private ArrayList<Slide> slides;
    private Style style;

    private ViewGroup root;
    private SmoothViewPager smoothViewPager;
    private ViewGroup bottomBarContainer;
    private AppCompatImageButton previous;
    private AppCompatImageButton next;
    private FrameLayout indicatorContainer;
    private Button skip;

    private IntroductionConfiguration configuration;

    private ButtonManager buttonManager;
    private IndicatorManager indicatorManager;

    private boolean showPreviousButton;
    private boolean showIndicator;
    private boolean allowBackPress;
    private String skipText;

    private int orientation;

    private int previousPagerPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getFieldsFromBundle();
        applyStyle();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.introduction_activity);

        configuration = IntroductionConfiguration.getInstance();

        findViews();
        initSlides();
        initManagers();
        initViews();

            if (savedInstanceState == null) {
                select(0);
                smoothViewPager.setCurrentItem(rtlAwarePosition(0), true);
            } else {
                previousPagerPosition = savedInstanceState.getInt(STATE_PREVIOUS_PAGER_POSITION);

                select(previousPagerPosition);
            }

            // Workarounds for fitsSystemWindows in a ViewPager.
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                WindowInsetsCompat newInsets = ViewCompat.onApplyWindowInsets(root, insets);

                if (newInsets.isConsumed()) {
                    return newInsets;
                }

                ViewCompat.dispatchApplyWindowInsets(bottomBarContainer, newInsets);

                return newInsets.isConsumed() ? newInsets.consumeSystemWindowInsets() : newInsets;
            });

            ViewCompat.setOnApplyWindowInsetsListener(smoothViewPager, (view, insets) -> {
                WindowInsetsCompat newInsets = ViewCompat.onApplyWindowInsets(view, insets);

                if (newInsets.isConsumed()) {
                    return newInsets;
                }

                boolean consumed = false;

                if (newInsets.isConsumed()) {
                    consumed = true;
                }

                for (int i = 0; i < smoothViewPager.getChildCount(); i++) {
                    ViewCompat.dispatchApplyWindowInsets(smoothViewPager.getChildAt(i), newInsets);

                    if (newInsets.isConsumed()) {
                        consumed = true;
                    }
                }

                return consumed ? newInsets.consumeSystemWindowInsets() : newInsets;
            });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(STATE_PREVIOUS_PAGER_POSITION, previousPagerPosition);
    }

    @NonNull
    Style getStyle() {
        return style;
    }

    private void getFieldsFromBundle() {
        Bundle bundle = getIntent().getExtras();

        if (bundle == null) {
            bundle = new Bundle();
        }

        slides = bundle.getParcelableArrayList(BUNDLE_SLIDES);
        style = (Style) bundle.getSerializable(BUNDLE_STYLE);
        orientation = bundle.getInt(BUNDLE_ORIENTATION, ORIENTATION_BOTH);
        showPreviousButton = bundle.getBoolean(BUNDLE_SHOW_PREVIOUS_BUTTON, true);
        showIndicator = bundle.getBoolean(BUNDLE_SHOW_INDICATOR, true);
        skipText = bundle.getString(BUNDLE_SKIP_STRING);
        allowBackPress = bundle.getBoolean(BUNDLE_ALLOW_BACK_PRESS, false);

        if (slides == null) {
            slides = new ArrayList<>();
        }

        if (skipText == null) {
            int skipResource = bundle.getInt(BUNDLE_SKIP_RESOURCE, 0);

            if (skipResource != 0) {
                skipText = this.getString(skipResource);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void applyStyle() {
        if (style != null) {
            style.applyStyle(this);

            if (orientation == ORIENTATION_PORTRAIT) {
                OrientationUtils.setOrientationPortrait(this);
            } else if (orientation == ORIENTATION_LANDSCAPE) {
                OrientationUtils.setOrientationLandscape(this);
            }
        }
    }

    private void findViews() {
        root = findViewById(R.id.introduction_activity_root);
        smoothViewPager = findViewById(R.id.introduction_activity_pager);
        indicatorContainer = findViewById(R.id.introduction_activity_container_indicator);
        bottomBarContainer = findViewById(R.id.introduction_activity_bottom_bar_container);
        skip = findViewById(R.id.introduction_activity_skip);

        if (OrientationUtils.isRTL(this)) {
            previous = findViewById(R.id.introduction_activity_button_next);
            next = findViewById(R.id.introduction_activity_button_previous);
        } else {
            previous = findViewById(R.id.introduction_activity_button_previous);
            next = findViewById(R.id.introduction_activity_button_next);
        }

        if (style != null) {
            style.applyStyleOnActivityView(this, root);
        }
    }

    private void initSlides() {
        if (OrientationUtils.isRTL(this)) {
            Collections.reverse(slides);
        }

        for (int i = 0; i < slides.size(); i++) {
            Slide slide = slides.get(i);

            slide.init(this, i);
        }
    }

    private void initManagers() {
        buttonManager = new ButtonManager(previous, next, skip, showPreviousButton, skipText != null, slides.size());
        indicatorManager = configuration.getIndicatorManager();

        if (indicatorManager == null && showIndicator) {
            indicatorManager = new DotIndicatorManager();
        }

        if (indicatorManager != null) {
            indicatorContainer.addView(indicatorManager.init(LayoutInflater.from(this),
                    indicatorContainer, slides.size()));

            indicatorManager.setListener(position -> smoothViewPager.setCurrentItem(position, true));
        }
    }

    private void initViews() {
        next.setOnClickListener(view -> {
            final int currentIndex = smoothViewPager.getCurrentItem();

            if (currentIndex == rtlAwarePosition(slides.size() - 1)) {
                handleFinish();
            } else {
                smoothViewPager.setCurrentItem(nextPosition(currentIndex), true);
            }
        });

        previous.setOnClickListener(view -> smoothViewPager.setCurrentItem(previousPosition(smoothViewPager.getCurrentItem()), true));

        smoothViewPager.setAdapter(new PagerAdapter(getSupportFragmentManager(), slides));
        smoothViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Not needed
            }

            @Override
            public void onPageSelected(int position) {
                if (position != previousPagerPosition) {
                    select(position);

                    IntroductionConfiguration.getInstance()
                            .callOnSlideChanged(previousPagerPosition, rtlAwarePosition(position));
                    previousPagerPosition = position;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // Not needed
            }
        });

        previous.bringToFront();
        next.bringToFront();

        if (configuration.getPageTransformer() != null) {
            smoothViewPager.setPageTransformer(true, configuration.getPageTransformer());
        }

        if (skipText != null) {
            skip.setText(skipText);
            skip.setOnClickListener(view -> handleFinish());
        }
    }

    private void select(int position) {
        if (indicatorManager != null) {
            indicatorManager.select(rtlAwarePosition(position));
        }

        if (buttonManager != null) {
            buttonManager.select(rtlAwarePosition(position));
        }
    }

    @Override
    public void onBackPressed() {
        if (smoothViewPager.getCurrentItem() == rtlAwarePosition(0)) {
            if (allowBackPress) {
                handleFinishCancelled();
            }
        } else {
            smoothViewPager.setCurrentItem(previousPosition(smoothViewPager.getCurrentItem()), true);
        }
    }

    private void handleFinish() {
        clean();
        ArrayList<Option> options = new ArrayList<>();

        for (Slide slide : slides) {
            if (slide.getOption() != null) {
                options.add(slide.getOption());
            }
        }

        Intent result = new Intent();

        result.putParcelableArrayListExtra(OPTION_RESULT, options);
        setResult(RESULT_OK, result);
        finish();
    }

    private void handleFinishCancelled() {
        clean();
        setResult(RESULT_CANCELED);
        finish();
    }

    private void clean() {
        if (indicatorManager != null) {
            indicatorManager.setListener(null);
        }

        IntroductionConfiguration.destroy();
    }

    private int rtlAwarePosition(final int position) {
        if (OrientationUtils.isRTL(this)) {
            return slides.size() - position - 1;
        } else {
            return position;
        }
    }

    private int nextPosition(final int position) {
        return OrientationUtils.isRTL(this) ? position - 1 : position + 1;
    }

    private int previousPosition(final int position) {
        return OrientationUtils.isRTL(this) ? position + 1 : position - 1;
    }
}
