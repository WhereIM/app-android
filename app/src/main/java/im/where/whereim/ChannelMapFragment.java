package im.where.whereim;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;

import im.where.whereim.dialogs.DialogCreateEnchantment;
import im.where.whereim.dialogs.DialogMapMenu;
import im.where.whereim.dialogs.DialogCreateMarker;
import im.where.whereim.dialogs.DialogOpenIn;
import im.where.whereim.dialogs.DialogShareLocation;
import im.where.whereim.models.Channel;
import im.where.whereim.models.Enchantment;
import im.where.whereim.models.Marker;
import im.where.whereim.models.Mate;
import im.where.whereim.models.POI;

/**
 * Created by buganini on 19/01/17.
 */

abstract public class ChannelMapFragment extends BaseFragment implements CoreService.MapDataDelegate {
    protected Handler mHandler = new Handler();

    protected View mMarkerActionsController;
    protected View mEnchantmentController;
    protected View mMarkerController;
    protected View mMarkerView;
    protected TextView mMarkerViewTitle;
    protected TextView mCreateMarker;
    protected TextView mCreateEnchantment;
    protected TextView mShare;
    protected TextView mOpenIn;

    protected double mEditingLatitude;
    protected double mEditingLongitude;

    protected TextView mEnchantment_radius;

    protected Marker mEditingMarker = new Marker();
    protected Enchantment mEditingEnchantment = new Enchantment();
    protected Key.MAP_OBJECT mEditingType = null;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMarkerActionsController = view.findViewById(R.id.marker_actions_controller);

        mCreateMarker = (TextView) view.findViewById(R.id.create_marker);
        mCreateMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DialogCreateMarker(getActivity(), focusTitle, new DialogCreateMarker.Callback() {
                    @Override
                    public void onCreateMarker(String name, boolean isPublic, JSONObject attr) {
                        clearMakerActionsController();

                        mEditingLatitude = focusLat;
                        mEditingLongitude = focusLng;

                        mEditingMarker.name = name;
                        mEditingMarker.isPublic = isPublic;
                        mEditingMarker.attr = attr;
                        mEditingType = Key.MAP_OBJECT.MARKER;
                        refreshEditing();
                    }
                });
            }
        });

        mCreateEnchantment= (TextView) view.findViewById(R.id.create_enchantment);
        mCreateEnchantment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DialogCreateEnchantment(getActivity(), focusTitle, new DialogCreateEnchantment.Callback() {
                    @Override
                    public void onPositive(String name, boolean isPublic) {
                        clearMakerActionsController();

                        mEditingLatitude = focusLat;
                        mEditingLongitude = focusLng;

                        mEditingEnchantment.name = name;
                        mEditingEnchantment.radius = Config.DEFAULT_ENCHANTMENT_RADIUS;
                        mEditingEnchantment.isPublic = isPublic;
                        mEnchantment_radius.setText(getString(R.string.radius_m, mEditingEnchantment.radius));
                        mEditingType = Key.MAP_OBJECT.ENCHANTMENT;
                        refreshEditing();
                    }
                });
            }
        });

        mShare = (TextView) view.findViewById(R.id.share);
        mShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DialogShareLocation(getActivity(), focusTitle, focusLat, focusLng);
            }
        });

        mOpenIn = (TextView) view.findViewById(R.id.open_in);
        mOpenIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DialogOpenIn(getActivity(), focusTitle, focusLat, focusLng);
            }
        });

        mEnchantmentController = view.findViewById(R.id.enchantment_controller);
        mEnchantment_radius = (TextView) view.findViewById(R.id.enchantment_radius);
        view.findViewById(R.id.enchantment_enlarge).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditingEnchantment.radius += Config.getRadiusStep(mEditingEnchantment.radius);
                mEditingEnchantment.radius = Math.min(Config.ENCHANTMENT_RADIUS_MAX, Math.max(Config.ENCHANTMENT_RADIUS_MIN, mEditingEnchantment.radius));
                mEnchantment_radius.setText(getString(R.string.radius_m, mEditingEnchantment.radius));
                refreshEditing();
            }
        });
        view.findViewById(R.id.enchantment_reduce).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditingEnchantment.radius -= Config.getRadiusStep(mEditingEnchantment.radius);
                mEditingEnchantment.radius = Math.min(Config.ENCHANTMENT_RADIUS_MAX, Math.max(Config.ENCHANTMENT_RADIUS_MIN, mEditingEnchantment.radius));
                mEnchantment_radius.setText(getString(R.string.radius_m, mEditingEnchantment.radius));
                refreshEditing();
            }
        });
        view.findViewById(R.id.enchantment_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditingType = null;
                refreshEditing();
            }
        });
        view.findViewById(R.id.enchantment_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(final CoreService.CoreBinder binder) {
                        getChannel(new ChannelActivity.GetChannelCallback() {
                            @Override
                            public void onGetChannel(Channel channel) {
                                mEditingEnchantment.channel_id = channel.id;
                                binder.setEnchantment(mEditingEnchantment);
                                mEditingType = null;
                                refreshEditing();
                            }
                        });
                    }
                });
            }
        });

        mMarkerController = view.findViewById(R.id.marker_controller);
        view.findViewById(R.id.marker_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(final CoreService.CoreBinder binder) {
                        getChannel(new ChannelActivity.GetChannelCallback() {
                            @Override
                            public void onGetChannel(Channel channel) {
                                mEditingMarker.channel_id = channel.id;
                                binder.setMarker(mEditingMarker);
                                mEditingType = null;
                                refreshEditing();
                            }
                        });
                    }
                });

            }
        });
        view.findViewById(R.id.marker_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditingType = null;
                refreshEditing();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    abstract protected void refreshEditing();

    protected void startEditing(){
        if(mEditingType!=null){
            if (mEditingType == Key.MAP_OBJECT.ENCHANTMENT) {
                mEditingEnchantment.latitude = mEditingLatitude;
                mEditingEnchantment.longitude = mEditingLongitude;
            }
            if (mEditingType == Key.MAP_OBJECT.MARKER) {
                mEditingMarker.latitude = mEditingLatitude;
                mEditingMarker.longitude = mEditingLongitude;
            }
            refreshEditing();
            return;
        }
        new DialogMapMenu(getActivity(), new DialogMapMenu.Callback() {
            @Override
            public void onOpenIn() {
                new DialogOpenIn(getActivity(), null, mEditingLatitude, mEditingLongitude);
            }

            @Override
            public void onShareLocation() {
                new DialogShareLocation(getActivity(), null, mEditingLatitude, mEditingLongitude);
            }

            @Override
            public void onCreateEnchantment() {
                new DialogCreateEnchantment(getActivity(), null, new DialogCreateEnchantment.Callback() {
                    @Override
                    public void onPositive(String name, boolean isPublic) {
                        clearMakerActionsController();
                        mEditingEnchantment.id = null;
                        mEditingEnchantment.radius = Config.DEFAULT_ENCHANTMENT_RADIUS;
                        mEditingEnchantment.name = name;
                        mEditingEnchantment.latitude = mEditingLatitude;
                        mEditingEnchantment.longitude = mEditingLongitude;
                        mEditingEnchantment.isPublic = isPublic;
                        mEnchantment_radius.setText(getString(R.string.radius_m, mEditingEnchantment.radius));
                        mEditingType = Key.MAP_OBJECT.ENCHANTMENT;
                        refreshEditing();
                    }
                });
            }

            @Override
            public void onCreateMarker() {
                new DialogCreateMarker(getActivity(), null, new DialogCreateMarker.Callback() {
                    @Override
                    public void onCreateMarker(String name, boolean isPublic, JSONObject attr) {
                        mEditingMarker.id = null;
                        mEditingMarker.name = name;
                        mEditingMarker.latitude = mEditingLatitude;
                        mEditingMarker.longitude = mEditingLongitude;
                        mEditingMarker.isPublic = isPublic;
                        mEditingMarker.attr = attr;
                        mEditingType = Key.MAP_OBJECT.MARKER;
                        refreshEditing();
                    }
                });
            }

            @Override
            public void onForgeLocation() {
                getChannel(new ChannelActivity.GetChannelCallback() {
                    @Override
                    public void onGetChannel(final Channel channel) {
                        postBinderTask(new CoreService.BinderTask() {
                            @Override
                            public void onBinderReady(CoreService.CoreBinder binder) {
                                binder.forgeLocation(channel, mEditingLatitude, mEditingLongitude);
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void editEnchantment(Enchantment enchantment) {
        mEditingType = Key.MAP_OBJECT.ENCHANTMENT;
        mEditingEnchantment.id = enchantment.id;
        mEditingEnchantment.name = enchantment.name;
        mEditingEnchantment.latitude = enchantment.latitude;
        mEditingEnchantment.longitude = enchantment.longitude;
        mEditingEnchantment.radius = enchantment.radius;
        mEditingEnchantment.isPublic = enchantment.isPublic;
        moveToEnchantment(enchantment);
        refreshEditing();
    }

    @Override
    public void editMarker(Marker marker) {
        mEditingType = Key.MAP_OBJECT.MARKER;
        mEditingMarker.id = marker.id;
        mEditingMarker.name = marker.name;
        mEditingMarker.latitude = marker.latitude;
        mEditingMarker.longitude = marker.longitude;
        mEditingMarker.attr = marker.attr;
        mEditingMarker.isPublic = marker.isPublic;
        moveToMarker(marker, false);
        refreshEditing();
    }

    private String focusTitle = null;
    private Double focusLat = null;
    private Double focusLng = null;
    protected void showMarkerActionsPanel(String title, double lat, double lng, boolean createMarker, boolean createEnchantment, boolean share, boolean open_in){
        focusTitle = title;
        focusLat = lat;
        focusLng = lng;
        mCreateMarker.setVisibility(createMarker?View.VISIBLE:View.GONE);
        mCreateEnchantment.setVisibility(createEnchantment?View.VISIBLE:View.GONE);
        mShare.setVisibility(share?View.VISIBLE:View.GONE);
        mOpenIn.setVisibility(open_in?View.VISIBLE:View.GONE);
        mMarkerActionsController.setVisibility(View.VISIBLE);
    }

    protected  void clearAction(boolean clearEditing){
        if(clearEditing) {
            mEditingType = null;
            refreshEditing();
        }
        mMarkerActionsController.setVisibility(View.GONE);
        mEnchantmentController.setVisibility(View.GONE);
        mMarkerController.setVisibility(View.GONE);
    }

    protected void clearMakerActionsController(){
        mMarkerActionsController.setVisibility(View.GONE);
    }



    public void clickMarker(Object obj) {
        if(obj==null){
            return;
        }
        String title;
        double latitude;
        double longitude;

        boolean mCreateMarker;
        boolean mCreateEnchantment;
        boolean mShare;
        boolean mOpenIn;
        if (obj instanceof Mate) {
            Mate mate = (Mate) obj;
            title = mate.getDisplayName();
            latitude = mate.latitude;
            longitude = mate.longitude;

            mCreateMarker = true;
            mCreateEnchantment = true;
            mShare = true;
            mOpenIn = true;
        } else if (obj instanceof im.where.whereim.models.Marker) {
            im.where.whereim.models.Marker marker = (im.where.whereim.models.Marker) obj;
            title = marker.name;
            latitude = marker.latitude;
            longitude = marker.longitude;

            mCreateMarker = false;
            mCreateEnchantment = true;
            mShare = true;
            mOpenIn = true;
        } else if (obj instanceof POI) {
            POI poi = (POI) obj;
            title = poi.name;
            latitude = poi.latitude;
            longitude = poi.longitude;

            mCreateMarker = true;
            mCreateEnchantment = true;
            mShare = true;
            mOpenIn = true;
        } else {
            return;
        }
        showMarkerActionsPanel(title, latitude, longitude, mCreateMarker, mCreateEnchantment, mShare, mOpenIn);
    }
}
