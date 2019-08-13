package com.milling.admin.adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.*;
import com.milling.admin.R;
import com.milling.admin.customViews.AnimatedExpandableListView;
import com.milling.admin.models.menu.InfinityItem;

import java.util.ArrayList;
import java.util.List;

public class InfinityListAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private int resId;
    static final int ANIMATION_DURATION = 1500;
    private  ArrayList<InfinityItem> infinityItems = null;
    private ListView parent;
    private ArrayList<InfinityItem> fullList = null;

    private int currentState = STATE_IDLE;

    private int totalSize = 0;
    private ArrayList<Boolean> visibilityList = null;
    private ArrayList<Boolean> expandedList = null;
    private SparseArray<GroupInfo> groupInfo = new SparseArray<GroupInfo>();

    private static final int STATE_IDLE = 0;
    private static final int STATE_EXPANDING = 1;
    private static final int STATE_COLLAPSING = 2;

    private int totalVisibleItems;
    private int childHeight;

    private int clickedPosition = -1;

    public InfinityListAdapter(Context context, int textViewResourceId, ArrayList<InfinityItem> objects) {
        this.resId = textViewResourceId;
        this.infinityItems = objects;
        this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.childHeight = -1;
        generateList();
    }

    public void setParent(ListView parent) {
        this.parent = parent;
    }

    private void generateList() {
        fullList = new ArrayList<>();
        visibilityList = new ArrayList<>();
        expandedList = new ArrayList<>();

        for(int i=0;i< infinityItems.size();i++)
        {
            InfinityItem parent =  infinityItems.get(i);
            parent.setParent(null);
            parent.setIndex(totalSize);
            fullList.add(parent);
            visibilityList.add(true);
            expandedList.add(false);
            totalSize++;
            insertChilds(parent);
        }
    }

    private void insertChilds(InfinityItem parent)
    {
        if(parent.getChilds()!=null)
            for(int j=0;j<parent.getChilds().size();j++)
            {
                InfinityItem child =  parent.getChilds().get(j);
                child.setParent(parent);
                child.setIndex(totalSize);
                fullList.add(child);
                visibilityList.add(false);
                expandedList.add(false);
                totalSize++;
                insertChilds(child);
            }
    }

    @Override
    public int getCount() {
        return fullList.size();
    }

    @Override
    public Object getItem(int position) {
        return fullList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    private int addVisibleChildsToDummyView(DummyView dummyView,int index,int measureSpecW,int measureSpecH,int clipHeight)
    {
        int totalHeight = 0;
        for(int i=0;i< fullList.get(index).getChilds().size();i++)
        {
            if(visibilityList.get(fullList.get(index).getChilds().get(i).getIndex()))
            {
                View childView = getRealViewAnimation(fullList.get(index).getChilds().get(i).getIndex(), null, parent);

                ViewGroup.LayoutParams p = (ViewGroup.LayoutParams) childView.getLayoutParams();
                if (p == null) {
                    p = (AbsListView.LayoutParams) generateDefaultLayoutParams();
                    childView.setLayoutParams(p);
                }

                int lpHeight = p.height;

                int childHeightSpec;
                if (lpHeight > 0) {
                    childHeightSpec = View.MeasureSpec.makeMeasureSpec(lpHeight, View.MeasureSpec.EXACTLY);
                } else {
                    childHeightSpec = measureSpecH;
                }

                childView.measure(measureSpecW, childHeightSpec);
                totalHeight += childView.getMeasuredHeight();

//                if (totalHeight < clipHeight) {
                    // we only need to draw enough views to fool the user...
                    dummyView.addFakeView(childView);
//                }

                totalHeight += addVisibleChildsToDummyView(dummyView,fullList.get(index).getChilds().get(i).getIndex(),measureSpecW,measureSpecH,clipHeight);
            }
        }
        return totalHeight;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if(position == clickedPosition)
            return  getRealViewAnimation(position, null, parent);

        if(clickedPosition == -1) {
            final GroupInfo info = getGroupInfo(position);
            return getRealView(position, convertView, parent);
        }

        final GroupInfo info = getGroupInfo(position);
        AbsListView.LayoutParams layoutParams=null;
        if (info.animating) {
            GroupInfo masterClicked = getGroupInfo(clickedPosition);
            if (convertView instanceof DummyView == false) {
                convertView = new DummyView(parent.getContext());
                convertView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
            }

            final DummyView dummyView = (DummyView) convertView;

            // Clear the views that the dummy view draws.
            dummyView.clearViews();
            final ListView listView = (ListView) parent;
            // Set the style of the divider
            dummyView.setDivider(listView.getDivider(), parent.getMeasuredWidth(), listView.getDividerHeight());

            final int measureSpecW = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
            final int measureSpecH = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

            int totalHeight = 0;
            int clipHeight = parent.getHeight();

            final int len = fullList.get(clickedPosition).getChilds().size();

            if(masterClicked.expanding == false)
            {
                for (int i = info.firstChildPosition; i < len; i++) {

                    if(visibilityList.get(fullList.get(clickedPosition).getChilds().get(i).getIndex()))
                    {
                        View childView = getRealViewAnimation(fullList.get(clickedPosition).getChilds().get(i).getIndex(), null, parent);

                        ViewGroup.LayoutParams p = (ViewGroup.LayoutParams) childView.getLayoutParams();
                        if (p == null) {
                            p = (AbsListView.LayoutParams) generateDefaultLayoutParams();
                            childView.setLayoutParams(p);
                        }

                        int lpHeight = p.height;

                        int childHeightSpec;
                        if (lpHeight > 0) {
                            childHeightSpec = View.MeasureSpec.makeMeasureSpec(lpHeight, View.MeasureSpec.EXACTLY);
                        } else {
                            childHeightSpec = measureSpecH;
                        }

                        childView.measure(measureSpecW, childHeightSpec);
                        totalHeight += childView.getMeasuredHeight();

                        if(childHeight == -1)
                            childHeight = childView.getMeasuredHeight();

//                        if (totalHeight < clipHeight) {
                            // we only need to draw enough views to fool the user...
                            dummyView.addFakeView(childView);
//                        } else {
//                            dummyView.addFakeView(childView);

                            // if this group has too many views, we don't want to
                            // calculate the height of everything... just do a light
                            // approximation and break
//                            int averageHeight = totalHeight / (i + 1);
//                            totalHeight += (len - i - 1) * averageHeight;
//                            break;
//                        }

                        totalHeight += addVisibleChildsToDummyView(dummyView,fullList.get(clickedPosition).getChilds().get(i).getIndex(),measureSpecW,measureSpecH,clipHeight);
                    }

                }
            }
            else {
                for (int i = info.firstChildPosition; i < len; i++) {
                    View childView = getRealViewAnimation(fullList.get(clickedPosition).getChilds().get(i).getIndex(), null, parent);

                    ViewGroup.LayoutParams p = (ViewGroup.LayoutParams) childView.getLayoutParams();
                    if (p == null) {
                        p = (AbsListView.LayoutParams) generateDefaultLayoutParams();
                        childView.setLayoutParams(p);
                    }

                    int lpHeight = p.height;

                    int childHeightSpec;
                    if (lpHeight > 0) {
                        childHeightSpec = View.MeasureSpec.makeMeasureSpec(lpHeight, View.MeasureSpec.EXACTLY);
                    } else {
                        childHeightSpec = measureSpecH;
                    }

                    childView.measure(measureSpecW, childHeightSpec);
                    totalHeight += childView.getMeasuredHeight();


                    if(childHeight == -1)
                        childHeight = childView.getMeasuredHeight();

//                    if (totalHeight < clipHeight) {
                        // we only need to draw enough views to fool the user...
                        dummyView.addFakeView(childView);
//                    } else {
//                        dummyView.addFakeView(childView);

                        // if this group has too many views, we don't want to
                        // calculate the height of everything... just do a light
                        // approximation and break
//                        int averageHeight = totalHeight / (i + 1);
//                        totalHeight += (len - i - 1) * averageHeight;
//                        break;
//                    }


                }
            }


            Object o;
            ViewHolder vh = (ViewHolder)dummyView.getTag();
            int state = STATE_IDLE;
            if(vh != null)
                state = vh.state;
            else {
                vh = new ViewHolder();
                vh.state = STATE_IDLE;
                vh.needInflate = true;
            }



            if (masterClicked.expanding && currentState != STATE_EXPANDING) {


                DropDownAnim ani2 = new DropDownAnim(this.parent, true,this.totalVisibleItems * this.childHeight,  totalHeight);
                ani2.setDuration(ANIMATION_DURATION);

                final int totalHh = totalHeight;
                ani2.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
//                        DropDownAnim ani = new DropDownAnim(dummyView,true, 0, totalHh);

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

//                this.parent.startAnimation(ani2);

//                this.parent.getLayoutParams().height = this.totalVisibleItems * this.childHeight + totalHeight;
//                this.parent.requestLayout();


                MyExpandAnimation ani = new MyExpandAnimation(dummyView, 0, totalHh, info);

                ani.setDuration(ANIMATION_DURATION);
                ani.setAnimationListener(new Animation.AnimationListener() {

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        stopAnimation(clickedPosition);
                        for(int i=0;i<fullList.get(clickedPosition).getChilds().size();i++)
                        {
                            stopAnimation(fullList.get(clickedPosition).getChilds().get(i).getIndex());
//                            GroupInfo info = getGroupInfo(fullList.get(clickedPosition).getChilds().get(i).getIndex());
//                            info.animating = false;
                        }
                        notifyDataSetChanged();
                        expandedList.set(clickedPosition,true);
                        masterClicked.animating = false;
                        ViewHolder vh = (ViewHolder)dummyView.getTag();
                        vh.state = STATE_IDLE;
                        currentState = STATE_IDLE;
                        dummyView.setTag(vh);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationStart(Animation animation) {
                        Log.i("xx","started");
                    }

                });
                dummyView.startAnimation(ani);

                vh.state = STATE_EXPANDING;
                currentState = STATE_EXPANDING;
                dummyView.setTag(vh);


            } else if (!masterClicked.expanding && currentState != STATE_COLLAPSING) {
                if (masterClicked.dummyHeight == -1) {
                    masterClicked.dummyHeight = totalHeight;
                }

                Log.i("xx","contracting");
                MyExpandAnimation ani = new MyExpandAnimation(dummyView, masterClicked.dummyHeight, 0, info);
                ani.setDuration(ANIMATION_DURATION);
                ani.setAnimationListener(new Animation.AnimationListener() {

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        stopAnimation(clickedPosition);
//                        listView.collapseGroup(groupPosition);
                        stopAnimatingChilds(clickedPosition);
                        notifyDataSetChanged();

                        masterClicked.dummyHeight = -1;
                        ViewHolder vh = (ViewHolder)dummyView.getTag();
                        vh.state = STATE_IDLE;
                        currentState = STATE_IDLE;
                        expandedList.set(clickedPosition,false);
                        dummyView.setTag(vh);
                        hideVisibleChilds(clickedPosition);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}

                    @Override
                    public void onAnimationStart(Animation animation) {}

                });
                dummyView.startAnimation(ani);
                vh.state = STATE_COLLAPSING;
                currentState = STATE_COLLAPSING;
                dummyView.setTag(vh);


            }

            return convertView;
        } else {
            return getRealView(position, convertView, parent);
        }
    }

    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 0);
    }

    public View getRealViewAnimation(final int position, View convertView, ViewGroup parent) {
        final View view;
        ViewHolder vh;
        InfinityItem item = (InfinityItem)getItem(position);

        if (convertView==null) {
            view = mInflater.inflate(R.layout.status_item_layout, parent, false);
            setViewHolder(view);
        }
        else if ((convertView.getTag()) == null) {
            view = mInflater.inflate(R.layout.status_item_layout, parent, false);
            setViewHolder(view);
        }
        else if (((ViewHolder)convertView.getTag()).needInflate) {
            view = mInflater.inflate(R.layout.status_item_layout, parent, false);
            setViewHolder(view);
        }
        else {
            view = convertView;
        }

        vh = (ViewHolder)view.getTag();
        vh.text.setText(item.getTitle());
        vh.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClick(view, position);
            }
        });


        return view;



    }


    public View getRealView(final int position, View convertView, ViewGroup parent) {
        final View view;
        ViewHolder vh;
        InfinityItem item = (InfinityItem)getItem(position);

        if (convertView==null) {
            view = mInflater.inflate(R.layout.status_item_layout, parent, false);
            setViewHolder(view);
        }
        else if ((convertView.getTag()) == null) {
            view = mInflater.inflate(R.layout.status_item_layout, parent, false);
            setViewHolder(view);
        }
        else if (((ViewHolder)convertView.getTag()).needInflate) {
            view = mInflater.inflate(R.layout.status_item_layout, parent, false);
            setViewHolder(view);
        }
        else {
            view = convertView;
        }

        vh = (ViewHolder)view.getTag();
        vh.text.setText(item.getTitle());
        vh.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClick(view, position);
            }
        });

        if(groupInfo.get(position).animating)
        {
            return mInflater.inflate(R.layout.null_item, parent,false);
        }
        else
        {
            if(visibilityList.get(position))
            {

                return view;
            }
            else
            {
                return mInflater.inflate(R.layout.null_item, parent,false);

            }
        }


    }


    private void onItemClick(final View v, final int index) {

        totalVisibleItems = 0;
        for(int i=0;i<visibilityList.size();i++)
            if(visibilityList.get(i))
                totalVisibleItems++;

        clickedPosition = index;
        if(!expandedList.get(index)) {
            startExpandAnimation(index, 0);
            for(int i=0;i<fullList.get(index).getChilds().size();i++)
            {
                visibilityList.set(fullList.get(index).getChilds().get(i).getIndex(),true);
//                startExpandAnimation(fullList.get(index).getChilds().get(i).getIndex(),0);
                GroupInfo info = getGroupInfo(fullList.get(clickedPosition).getChilds().get(i).getIndex());
                info.animating = true;
            }
            notifyDataSetChanged();
        }
        else {
            startCollapseAnimation(index, 0);
            startCollapsingVisibleChilds(index);
            notifyDataSetChanged();
        }


    }


    private void expand(final View v, Animation.AnimationListener al,final int index) {
        final int startHeight = v.getMeasuredHeight();
        final int finalHeight = startHeight * fullList.get(index).getChilds().size();
        expandedList.set(index,true);
//        for(int i=0;i<fullList.get(index).getChilds().size();i++)
//        {
//            visibilityList.set(fullList.get(index).getChilds().get(i).getIndex(),true);
//        }

        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.VISIBLE);

                }
                else {
                    v.getLayoutParams().height = startHeight + (int)(finalHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        if (al!=null) {
            anim.setAnimationListener(al);
        }
        anim.setDuration(ANIMATION_DURATION);
        v.startAnimation(anim);
    }

    private int getVisibleChilds(int index)
    {
        int visible = 0;
        for(int i=0;i< fullList.get(index).getChilds().size();i++)
        {
            if(visibilityList.get(fullList.get(index).getChilds().get(i).getIndex()))
            {
                visible = visible + 1 +  getVisibleChilds(fullList.get(index).getChilds().get(i).getIndex());
            }
        }
        return visible;
    }

    private int startCollapsingVisibleChilds(int index)
    {
        int visible = 0;
        for(int i=0;i< fullList.get(index).getChilds().size();i++)
        {
            if(visibilityList.get(fullList.get(index).getChilds().get(i).getIndex()))
            {
                startCollapseAnimation(fullList.get(index).getChilds().get(i).getIndex(),0);
                startCollapsingVisibleChilds(fullList.get(index).getChilds().get(i).getIndex());
            }
        }
        return visible;
    }

    private int hideVisibleChilds(int index)
    {
        int visible = 0;
        for(int i=0;i< fullList.get(index).getChilds().size();i++)
        {
            if(visibilityList.get(fullList.get(index).getChilds().get(i).getIndex()))
            {
                visibilityList.set(fullList.get(index).getChilds().get(i).getIndex(),false);
                expandedList.set(fullList.get(index).getChilds().get(i).getIndex(),false);
                hideVisibleChilds(fullList.get(index).getChilds().get(i).getIndex());
            }
        }
        return visible;
    }

    private int stopAnimatingChilds(int index)
    {
        int visible = 0;
        for(int i=0;i< fullList.get(index).getChilds().size();i++)
        {
            stopAnimation(fullList.get(index).getChilds().get(i).getIndex());
            stopAnimatingChilds(fullList.get(index).getChilds().get(i).getIndex());
        }
        return visible;
    }

    private void collapse(final View v, Animation.AnimationListener al,final int index) {
        expandedList.set(index,false);
        final int itemHeight = v.getMeasuredHeight();
        final int finalHeight = itemHeight * getVisibleChilds(index);
        hideVisibleChilds(index);
//        final int initialHeight = v.getMeasuredHeight();

        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                }
                else {
                    v.getLayoutParams().height = finalHeight - (int)(finalHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        if (al!=null) {
            anim.setAnimationListener(al);
        }
        anim.setDuration(ANIMATION_DURATION);
        v.startAnimation(anim);
    }

    private void setViewHolder(View view) {
        ViewHolder vh = new ViewHolder();
        vh.text = (TextView)view.findViewById(R.id.status_item_name);
        vh.linearLayout = (LinearLayout)view.findViewById(R.id.status_item);
        vh.needInflate = false;
        view.setTag(vh);
    }

    private class ViewHolder {
        public boolean needInflate;
        public TextView text;
        public LinearLayout linearLayout;
        public Integer state;
    }



    private static class GroupInfo {
        boolean animating = false;
        boolean expanding = false;
        int firstChildPosition;

        /**
         * This variable contains the last known height value of the dummy view.
         * We save this information so that if the user collapses a group
         * before it fully expands, the collapse animation will start from the
         * CURRENT height of the dummy view and not from the full expanded
         * height.
         */
        int dummyHeight = -1;
    }

    private GroupInfo getGroupInfo(int groupPosition) {
        GroupInfo info = groupInfo.get(groupPosition);
        if (info == null) {
            info = new GroupInfo();
            groupInfo.put(groupPosition, info);
        }
        return info;
    }


    private static class DummyView extends View {
        public List<View> views = new ArrayList<View>();
        private Drawable divider;
        private int dividerWidth;
        private int dividerHeight;

        public DummyView(Context context) {
            super(context);
        }

        public void setDivider(Drawable divider, int dividerWidth, int dividerHeight) {
            if(divider != null) {
                this.divider = divider;
                this.dividerWidth = dividerWidth;
                this.dividerHeight = dividerHeight;

                divider.setBounds(0, 0, dividerWidth, dividerHeight);
            }
        }

        /**
         * Add a view for the DummyView to draw.
         * @param childView View to draw
         */
        public void addFakeView(View childView) {
            childView.layout(0, 0, getWidth(), childView.getMeasuredHeight());
            views.add(childView);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            final int len = views.size();
            for(int i = 0; i < len; i++) {
                View v = views.get(i);
                v.layout(left, top, left + v.getMeasuredWidth(), top + v.getMeasuredHeight());
            }
        }

        public void clearViews() {
            views.clear();
        }

        @Override
        public void dispatchDraw(Canvas canvas) {
            canvas.save();
            if(divider != null) {
                divider.setBounds(0, 0, dividerWidth, dividerHeight);
            }

            final int len = views.size();
            for(int i = 0; i < len; i++) {
                View v = views.get(i);

                canvas.save();
                canvas.clipRect(0, 0, getWidth(), v.getMeasuredHeight());
                v.draw(canvas);
                canvas.restore();

                if(divider != null) {
                    divider.draw(canvas);
                    canvas.translate(0, dividerHeight);
                }

                canvas.translate(0, v.getMeasuredHeight());
            }

            canvas.restore();
        }
    }

    private static class MyExpandAnimation extends Animation {
        private int baseHeight;
        private int delta;
        private View view;
        private GroupInfo groupInfo;

        private MyExpandAnimation(View v, int startHeight, int endHeight, GroupInfo info) {
            baseHeight = startHeight;
            delta = endHeight - startHeight;
            view = v;
            groupInfo = info;
            view.getLayoutParams().height = startHeight;
            view.requestLayout();
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            if (interpolatedTime < 1.0f) {
                int val = baseHeight + (int) (delta * interpolatedTime);
                view.getLayoutParams().height = val;
                if(groupInfo!=null)
                    groupInfo.dummyHeight = val;
                view.requestLayout();
            } else {
                Log.i("xx","animatie terminata");
                int val = baseHeight + delta;
                view.getLayoutParams().height = val;
                if(groupInfo!=null)
                    groupInfo.dummyHeight = val;
                view.requestLayout();
            }
        }
    }


    public void notifyGroupExpanded(int groupPosition) {
        GroupInfo info = getGroupInfo(groupPosition);
        info.dummyHeight = -1;
    }

    private void startExpandAnimation(int groupPosition, int firstChildPosition) {
        GroupInfo info = getGroupInfo(groupPosition);
        info.animating = true;
        info.firstChildPosition = firstChildPosition;
        info.expanding = true;
    }

    private void startCollapseAnimation(int groupPosition, int firstChildPosition) {
        GroupInfo info = getGroupInfo(groupPosition);
        info.animating = true;
        info.firstChildPosition = firstChildPosition;
        info.expanding = false;
    }

    private void stopAnimation(int groupPosition) {
        GroupInfo info = getGroupInfo(groupPosition);
        info.animating = false;
    }

    public class DropDownAnim extends Animation {
        private final int targetHeight;
        private final int baseHeight;
        private final View view;
        private final boolean expand;

        public DropDownAnim(View view, boolean expand,int baseHeight, int targetHeight) {
            this.view = view;
            this.targetHeight = targetHeight;
            this.baseHeight = baseHeight;
            this.expand = expand;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            int newHeight;
            if (expand) {
                newHeight = (int) (baseHeight + targetHeight * interpolatedTime);
            } else {
                newHeight = (int) (baseHeight + targetHeight * (1 - interpolatedTime));
            }
            view.getLayoutParams().height = newHeight;
            view.requestLayout();
        }

        @Override
        public void initialize(int width, int height, int parentWidth,
                               int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }
    }
}


