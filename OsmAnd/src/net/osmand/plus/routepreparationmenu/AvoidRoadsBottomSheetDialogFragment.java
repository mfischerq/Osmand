package net.osmand.plus.routepreparationmenu;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitleDividerItem;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.router.GeneralRouter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AvoidRoadsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = AvoidRoadsBottomSheetDialogFragment.class.getSimpleName();

	public static final int REQUEST_CODE = 0;
	public static final int OPEN_AVOID_ROADS_DIALOG_REQUEST_CODE = 1;

	private static final String AVOID_ROADS_TYPES_KEY = "avoid_roads_types";
	private static final String AVOID_ROADS_OBJECTS_KEY = "avoid_roads_objects";

	private HashMap<String, Boolean> routingParametersMap;
	private List<LatLon> removedImpassableRoads;
	private LinearLayout stylesContainer;

	List<BottomSheetItemWithCompoundButton> compoundButtons = new ArrayList<>();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(AVOID_ROADS_TYPES_KEY)) {
				routingParametersMap = (HashMap<String, Boolean>) savedInstanceState.getSerializable(AVOID_ROADS_TYPES_KEY);
			}
			if (savedInstanceState.containsKey(AVOID_ROADS_OBJECTS_KEY)) {
				removedImpassableRoads = (List<LatLon>) savedInstanceState.getSerializable(AVOID_ROADS_OBJECTS_KEY);
			}
		}
		if (routingParametersMap == null) {
			routingParametersMap = generateStylesMap(app);
		}
		if (removedImpassableRoads == null) {
			removedImpassableRoads = new ArrayList<LatLon>();
		}

		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		final View titleView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.bottom_sheet_item_toolbar_title, null);
		TextView textView = (TextView) titleView.findViewById(R.id.title);
		textView.setText(R.string.impassable_road);

		Toolbar toolbar = (Toolbar) titleView.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getContentIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		final SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(titleView)
				.create();
		items.add(titleItem);

		final SimpleBottomSheetItem descriptionItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.avoid_roads_descr))
				.setLayoutId(R.layout.bottom_sheet_item_title_long)
				.create();
		items.add(descriptionItem);

		stylesContainer = new LinearLayout(app);
		stylesContainer.setLayoutParams((new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)));
		stylesContainer.setOrientation(LinearLayout.VERTICAL);
		stylesContainer.setPadding(0, getResources().getDimensionPixelSize(R.dimen.bottom_sheet_content_padding_small), 0, 0);

		for (final LatLon routeDataObject : app.getAvoidSpecificRoads().getImpassableRoads().keySet()) {
			if (removedImpassableRoads.contains(routeDataObject)) {
				continue;
			}
			LayoutInflater.from(new ContextThemeWrapper(app, themeRes)).inflate(R.layout.bottom_sheet_item_simple_right_icon, stylesContainer, true);
		}
		items.add(new BaseBottomSheetItem.Builder().setCustomView(stylesContainer).create());

		populateImpassableRoadsObjects();

		final View buttonView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.bottom_sheet_item_btn, null);
		TextView buttonDescription = (TextView) buttonView.findViewById(R.id.button_descr);
		buttonDescription.setText(R.string.shared_string_select_on_map);
		buttonDescription.setTextColor(getResolvedColor(nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light));

		FrameLayout buttonContainer = buttonView.findViewById(R.id.button_container);
		AndroidUtils.setBackground(app, buttonContainer, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, buttonDescription, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		}

		buttonContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					mapActivity.getDashboard().setDashboardVisibility(false, DashboardOnMap.DashboardType.ROUTE_PREFERENCES);
					mapActivity.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu().hide();
					app.getAvoidSpecificRoads().selectFromMap(mapActivity);
					Fragment fragment = getTargetFragment();
					if (fragment != null) {
						fragment.onActivityResult(getTargetRequestCode(), OPEN_AVOID_ROADS_DIALOG_REQUEST_CODE, null);
					}
				}
				dismiss();
			}
		});

		final SimpleBottomSheetItem buttonItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(buttonView)
				.create();
		items.add(buttonItem);

		items.add(new SubtitleDividerItem(app));

		populateImpassableRoadsTypes();
	}

	private void populateImpassableRoadsObjects() {
		Context context = getContext();
		if (context == null) {
			return;
		}
		AvoidSpecificRoads avoidSpecificRoads = getMyApplication().getAvoidSpecificRoads();

		int counter = 0;
		for (final LatLon routeDataObject : avoidSpecificRoads.getImpassableRoads().keySet()) {
			if (removedImpassableRoads.contains(routeDataObject)) {
				continue;
			}
			String name = avoidSpecificRoads.getText(routeDataObject);

			View view = stylesContainer.getChildAt(counter);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					removedImpassableRoads.add(routeDataObject);
					stylesContainer.removeView(v);
				}
			});

			TextView titleTv = (TextView) view.findViewById(R.id.title);
			titleTv.setText(name);
			titleTv.setTextColor(getResolvedColor(nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light));

			ImageView icon = (ImageView) view.findViewById(R.id.icon);
			icon.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));

			counter++;
		}
	}

	private void populateImpassableRoadsTypes() {
		Context context = getContext();
		if (context == null) {
			return;
		}
		for (Map.Entry<String, Boolean> entry : routingParametersMap.entrySet()) {
			final String parameterId = entry.getKey();
			boolean selected = entry.getValue();
			String parameterName = SettingsBaseActivity.getRoutingStringPropertyName(context, parameterId, "");

			final BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
			item[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(selected)
					.setTitle(parameterName)
					.setLayoutId(R.layout.bottom_sheet_item_with_switch_no_icon)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							item[0].setChecked(!item[0].isChecked());
							routingParametersMap.put(parameterId, item[0].isChecked());
						}
					})
					.setTag(parameterId)
					.create();
			items.add(item[0]);
			compoundButtons.add(item[0]);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		for (BottomSheetItemWithCompoundButton item : compoundButtons) {
			final String routingParameterId = (String) item.getTag();
			item.setChecked(routingParametersMap.get(routingParameterId));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(AVOID_ROADS_TYPES_KEY, routingParametersMap);
		outState.putSerializable(AVOID_ROADS_OBJECTS_KEY, (Serializable) removedImpassableRoads);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}


	@Nullable
	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity != null && activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	@Override
	protected void onRightBottomButtonClick() {
		final OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}

		for (Map.Entry<String, Boolean> entry : routingParametersMap.entrySet()) {
			String parameterId = entry.getKey();
			GeneralRouter.RoutingParameter parameter = app.getRoutingOptionsHelper().getRoutingPrefsForAppModeById(app.getRoutingHelper().getAppMode(), parameterId);
			if (parameter != null) {
				boolean checked = entry.getValue();
				OsmandSettings.CommonPreference<Boolean> preference = app.getSettings().getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());
				preference.set(checked);
			}
		}

		AvoidSpecificRoads avoidSpecificRoads = app.getAvoidSpecificRoads();
		for (LatLon routeLocation : removedImpassableRoads) {
			avoidSpecificRoads.removeImpassableRoad(routeLocation);
		}

		RoutingHelper routingHelper = app.getRoutingHelper();
		if (routingHelper.isRouteCalculated() || routingHelper.isRouteBeingCalculated()) {
			routingHelper.recalculateRouteDueToSettingsChange();
		}

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			final MapRouteInfoMenu mapRouteInfoMenu = mapActivity.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu();
			if (mapRouteInfoMenu != null) {
				mapRouteInfoMenu.updateMenu();
			}
		}

		dismiss();
	}

	@NonNull
	private HashMap<String, Boolean> generateStylesMap(OsmandApplication app) {
		HashMap<String, Boolean> res = new HashMap<>();
		List<GeneralRouter.RoutingParameter> avoidParameters = app.getRoutingOptionsHelper().getAvoidRoutingPrefsForAppMode(app.getRoutingHelper().getAppMode());

		for (GeneralRouter.RoutingParameter parameter : avoidParameters) {
			OsmandSettings.CommonPreference<Boolean> preference = app.getSettings().getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());
			res.put(parameter.getId(), preference.get());
		}

		return res;
	}
}