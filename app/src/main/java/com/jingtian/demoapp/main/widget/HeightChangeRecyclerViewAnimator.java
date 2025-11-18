package com.jingtian.demoapp.main.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.ArrayList;
import java.util.List;

public class HeightChangeRecyclerViewAnimator extends SimpleItemAnimator {

    private static final boolean ENABLE_DEBUG_MODE = false;
    private static final boolean DEBUG = ENABLE_DEBUG_MODE;
    @NonNull
    private final View parent;
    private final RecyclerView recyclerView;
    private static final TimeInterpolator sDefaultInterpolator = new AccelerateDecelerateInterpolator();

    private final ArrayList<ViewHolder> mPendingRemovals = new ArrayList<>();
    private final ArrayList<Integer> mPendingParentChange = new ArrayList<>();
    private final ArrayList<ViewHolder> mPendingAdditions = new ArrayList<>();
    private final ArrayList<MoveInfo> mPendingMoves = new ArrayList<>();
    private final ArrayList<ChangeInfo> mPendingChanges = new ArrayList<>();

    ArrayList<ArrayList<ViewHolder>> mAdditionsList = new ArrayList<>();
    ArrayList<ArrayList<MoveInfo>> mMovesList = new ArrayList<>();
    ArrayList<ArrayList<ChangeInfo>> mChangesList = new ArrayList<>();

    ArrayList<ViewHolder> mAddAnimations = new ArrayList<>();
    ArrayList<ViewHolder> mMoveAnimations = new ArrayList<>();
    ArrayList<ViewHolder> mRemoveAnimations = new ArrayList<>();
    ArrayList<ViewHolder> mChangeAnimations = new ArrayList<>();
    ArrayList<ParentChangeInfo> mParentChangeAnimations = new ArrayList<>();

    public HeightChangeRecyclerViewAnimator(@NonNull RecyclerView recyclerView) {
        this.parent = (View) recyclerView.getParent();
        this.recyclerView = recyclerView;
        setLayoutParamsHeight(this.parent, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParamsHeight(this.recyclerView, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static class ParentChangeInfo {
        @NonNull
        public final ObjectAnimator anim;
        @NonNull
        public final AnimatedObj animatedObj;
        public boolean isCancel = false;
        ParentChangeInfo(@NonNull ObjectAnimator anim, @NonNull AnimatedObj animatedObj) {
            this.anim = anim;
            this.animatedObj = animatedObj;
        }
    }

    private static class MoveInfo {
        public ViewHolder holder;
        public int fromX, fromY, toX, toY;

        MoveInfo(ViewHolder holder, int fromX, int fromY, int toX, int toY) {
            this.holder = holder;
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }
    }

    private static class ChangeInfo {
        public ViewHolder oldHolder, newHolder;
        public int fromX, fromY, toX, toY;

        private ChangeInfo(ViewHolder oldHolder, ViewHolder newHolder) {
            this.oldHolder = oldHolder;
            this.newHolder = newHolder;
        }

        ChangeInfo(ViewHolder oldHolder, ViewHolder newHolder, int fromX, int fromY, int toX, int toY) {
            this(oldHolder, newHolder);
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }

        @NonNull
        @Override
        public String toString() {
            return "ChangeInfo{" + "oldHolder=" + oldHolder + ", newHolder=" + newHolder + ", fromX=" + fromX + ", fromY=" + fromY + ", toX=" + toX + ", toY=" + toY + '}';
        }
    }

    @Override
    public void runPendingAnimations() {
        final int parentHeight = parent.getHeight();
        int removalHeightChange = 0;
        int changeHeightChange = 0;
        boolean removalsPending = !mPendingRemovals.isEmpty();
        boolean movesPending = !mPendingMoves.isEmpty();
        boolean changesPending = !mPendingChanges.isEmpty();
        boolean additionsPending = !mPendingAdditions.isEmpty();
        if (!removalsPending && !movesPending && !additionsPending && !changesPending) {
            // nothing to animate
            return;
        }
        // First, remove stuff
        for (ViewHolder holder : mPendingRemovals) {
            animateRemoveImpl(holder);
            removalHeightChange += getViewHeightWithMargins(holder.itemView);
        }
        final ArrayList<ChangeInfo> changes = new ArrayList<>(mPendingChanges);
        mChangesList.add(changes);
        mPendingChanges.clear();
        for (ChangeInfo change : changes) {
            changeHeightChange += getViewHeightWithMargins(change.newHolder) - getViewHeightWithMargins(change.oldHolder);
        }

        final ArrayList<ViewHolder> additions = new ArrayList<>(mPendingAdditions);
        mAdditionsList.add(additions);
        mPendingAdditions.clear();
        int addHeightChange = 0;
        for (ViewHolder holder : additions) {
            addHeightChange += getViewHeightWithMargins(holder);
        }

        final int finalHeight;
        final int initialHeight;

        if (!mParentChangeAnimations.isEmpty()) {
            ParentChangeInfo animationInfo = mParentChangeAnimations.get(0);
            mParentChangeAnimations.clear();
            int targetHeight = animationInfo.animatedObj.finalHeight;
            finalHeight = targetHeight - removalHeightChange + changeHeightChange + addHeightChange;
            initialHeight = animationInfo.animatedObj.currentHeight;
        } else {
            if (!mPendingParentChange.isEmpty()) {
                initialHeight = mPendingParentChange.get(0);
                finalHeight = initialHeight - removalHeightChange + changeHeightChange + addHeightChange;
                mPendingParentChange.clear();
            } else {
                finalHeight = parentHeight;
                initialHeight = parentHeight + removalHeightChange - changeHeightChange - addHeightChange;
            }
        }
        updateHeight(initialHeight);
        if (initialHeight != finalHeight) {
            parentChangeAnimImpl(initialHeight, finalHeight, finalHeight, getMoveDuration());
            movesPending = true;
        }

        mPendingRemovals.clear();
        if (movesPending) {
            final ArrayList<MoveInfo> moves = new ArrayList<>(mPendingMoves);
            mMovesList.add(moves);
            mPendingMoves.clear();
            Runnable mover = () -> {
                for (MoveInfo moveInfo : moves) {
                    animateMoveImpl(moveInfo.holder, moveInfo.fromX, moveInfo.fromY, moveInfo.toX, moveInfo.toY);
                }
                startParentAnim();
                moves.clear();
                mMovesList.remove(moves);
            };
            if (removalsPending) {
                if (moves.isEmpty()) {
                    parent.postOnAnimationDelayed(mover, getRemoveDuration());
                } else {
                    View view = moves.get(0).holder.itemView;
                    view.postOnAnimationDelayed(mover, getRemoveDuration());
                }
            } else {
                mover.run();
            }
        }
        // Next, change stuff, to run in parallel with move animations
        if (changesPending) {
            Runnable changer = () -> {
                for (ChangeInfo change : changes) {
                    animateChangeImpl(change);
                }
                changes.clear();
                mChangesList.remove(changes);
            };
            if (removalsPending) {
                ViewHolder holder = changes.get(0).oldHolder;
                holder.itemView.postOnAnimationDelayed(changer, getRemoveDuration());
            } else {
                changer.run();
            }
        }
        // Next, add stuff
        if (additionsPending) {
            Runnable adder = () -> {
                for (ViewHolder holder : additions) {
                    animateAddImpl(holder);
                }
                additions.clear();
                mAdditionsList.remove(additions);
            };
            if (removalsPending || movesPending || changesPending) {
                long removeDuration = removalsPending ? getRemoveDuration() : 0;
                long moveDuration = movesPending ? getMoveDuration() : 0;
                long changeDuration = changesPending ? getChangeDuration() : 0;
                long totalDelay = removeDuration + Math.max(moveDuration, changeDuration);
                View view = additions.get(0).itemView;
                view.postOnAnimationDelayed(adder, totalDelay);
            } else {
                adder.run();
            }
        }
    }

    @Override
    public boolean animateRemove(final ViewHolder holder) {
        resetAnimation(holder);
        mPendingRemovals.add(holder);
        if (!mParentChangeAnimations.isEmpty()) {
            ParentChangeInfo anim = mParentChangeAnimations.get(0);
            anim.anim.removeAllListeners();
            anim.anim.cancel();
            anim.isCancel = true;
            updateHeight(anim.animatedObj.currentHeight);
            blockNextDraw(parent);
        } else if (mPendingParentChange.isEmpty()) {
            int delta = getViewHeightWithMargins(holder);
            mPendingParentChange.add(delta + parent.getHeight());
            changeHeight(delta);
            blockNextDraw(parent);
        } else {
            int delta = getViewHeightWithMargins(holder);
            int targetHeight = mPendingParentChange.get(0) + delta;
            mPendingParentChange.set(0, targetHeight);
            updateHeight(targetHeight);
            blockNextDraw(parent);
        }
        return true;
    }

    private void animateRemoveImpl(final ViewHolder holder) {
        final View view = holder.itemView;
        final ViewPropertyAnimator animation = view.animate();
        mRemoveAnimations.add(holder);
        animation.setDuration(getRemoveDuration()).alpha(0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                dispatchRemoveStarting(holder);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animation.setListener(null);
                view.setAlpha(1);
                dispatchRemoveFinished(holder);
                mRemoveAnimations.remove(holder);
                dispatchFinishedWhenDone();
            }
        }).start();
    }

    @Override
    public boolean animateAdd(final ViewHolder holder) {
        resetAnimation(holder);
        holder.itemView.setAlpha(0);
        mPendingAdditions.add(holder);
        if (!mParentChangeAnimations.isEmpty()) {
            ParentChangeInfo anim = mParentChangeAnimations.get(0);
            anim.anim.removeAllListeners();
            anim.anim.cancel();
            anim.isCancel = true;
            updateHeight(anim.animatedObj.currentHeight);
            blockNextDraw(parent);
        } else if (mPendingParentChange.isEmpty()) {
            int delta = -getViewHeightWithMargins(holder);
            mPendingParentChange.add(parent.getHeight() + delta);
            changeHeight(delta);
            blockNextDraw(parent);
        } else {
            int delta = -getViewHeightWithMargins(holder);
            int targetHeight = mPendingParentChange.get(0) + delta;
            mPendingParentChange.set(0, targetHeight);
            updateHeight(targetHeight);
            blockNextDraw(parent);
        }
        return true;
    }

    void animateAddImpl(final ViewHolder holder) {
        final View view = holder.itemView;
        final ViewPropertyAnimator animation = view.animate();
        mAddAnimations.add(holder);
        animation.alpha(1).setDuration(getAddDuration()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                dispatchAddStarting(holder);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                view.setAlpha(1);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animation.setListener(null);
                dispatchAddFinished(holder);
                mAddAnimations.remove(holder);
                dispatchFinishedWhenDone();
            }
        }).start();
    }

    @Override
    public boolean animateMove(final ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        final View view = holder.itemView;
        fromX += (int) holder.itemView.getTranslationX();
        fromY += (int) holder.itemView.getTranslationY();
        resetAnimation(holder);
        int deltaX = toX - fromX;
        int deltaY = toY - fromY;
        if (deltaX == 0 && deltaY == 0) {
            dispatchMoveFinished(holder);
            return false;
        }
        if (deltaX != 0) {
            view.setTranslationX(-deltaX);
        }
        if (deltaY != 0) {
            view.setTranslationY(-deltaY);
        }
        mPendingMoves.add(new MoveInfo(holder, fromX, fromY, toX, toY));
        return true;
    }

    void animateMoveImpl(final ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        final View view = holder.itemView;
        final int deltaX = toX - fromX;
        final int deltaY = toY - fromY;
        if (deltaX != 0) {
            view.animate().translationX(0);
        }
        if (deltaY != 0) {
            view.animate().translationY(0);
        }
        final ViewPropertyAnimator animation = view.animate();
        mMoveAnimations.add(holder);
        animation.setDuration(getMoveDuration()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                dispatchMoveStarting(holder);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                if (deltaX != 0) {
                    view.setTranslationX(0);
                }
                if (deltaY != 0) {
                    view.setTranslationY(0);
                }
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animation.setListener(null);
                dispatchMoveFinished(holder);
                mMoveAnimations.remove(holder);
                dispatchFinishedWhenDone();
            }
        }).start();
    }

    @Override
    public boolean animateChange(ViewHolder oldHolder, ViewHolder newHolder, int fromX, int fromY, int toX, int toY) {
        if (oldHolder == newHolder) {
            // Don't know how to run change animations when the same view holder is re-used.
            // run a move animation to handle position changes.
            return animateMove(oldHolder, fromX, fromY, toX, toY);
        }
        if (!mParentChangeAnimations.isEmpty()) {
            ParentChangeInfo anim = mParentChangeAnimations.get(0);
            anim.anim.removeAllListeners();
            anim.anim.cancel();
            anim.isCancel = true;
            updateHeight(anim.animatedObj.currentHeight);
            blockNextDraw(parent);
        } else if (mPendingParentChange.isEmpty()) {
            int delta = getViewHeightWithMargins(oldHolder) - getViewHeightWithMargins(newHolder);
            mPendingParentChange.add(parent.getHeight() + delta);
            changeHeight(delta);
            blockNextDraw(parent);
        } else {
            int delta = getViewHeightWithMargins(oldHolder) - getViewHeightWithMargins(newHolder);
            int targetHeight = mPendingParentChange.get(0) + delta;
            mPendingParentChange.set(0, targetHeight);
            updateHeight(targetHeight);
            blockNextDraw(parent);
        }
        final float prevTranslationX = oldHolder.itemView.getTranslationX();
        final float prevTranslationY = oldHolder.itemView.getTranslationY();
        final float prevAlpha = oldHolder.itemView.getAlpha();
        resetAnimation(oldHolder);
        int deltaX = (int) (toX - fromX - prevTranslationX);
        int deltaY = (int) (toY - fromY - prevTranslationY);
        // recover prev translation state after ending animation
        oldHolder.itemView.setTranslationX(prevTranslationX);
        oldHolder.itemView.setTranslationY(prevTranslationY);
        oldHolder.itemView.setAlpha(prevAlpha);
        if (newHolder != null) {
            // carry over translation values
            resetAnimation(newHolder);
            newHolder.itemView.setTranslationX(-deltaX);
            newHolder.itemView.setTranslationY(-deltaY);
            newHolder.itemView.setAlpha(0);
        }
        mPendingChanges.add(new ChangeInfo(oldHolder, newHolder, fromX, fromY, toX, toY));
        return true;
    }

    void animateChangeImpl(final ChangeInfo changeInfo) {
        final ViewHolder holder = changeInfo.oldHolder;
        final View view = holder == null ? null : holder.itemView;
        final ViewHolder newHolder = changeInfo.newHolder;
        final View newView = newHolder != null ? newHolder.itemView : null;
        if (view != null) {
            final ViewPropertyAnimator oldViewAnim = view.animate().setDuration(getChangeDuration());
            mChangeAnimations.add(changeInfo.oldHolder);
            oldViewAnim.translationX(changeInfo.toX - changeInfo.fromX);
            oldViewAnim.translationY(changeInfo.toY - changeInfo.fromY);
            oldViewAnim.alpha(0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator) {
                    dispatchChangeStarting(changeInfo.oldHolder, true);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    oldViewAnim.setListener(null);
                    view.setAlpha(1);
                    view.setTranslationX(0);
                    view.setTranslationY(0);
                    dispatchChangeFinished(changeInfo.oldHolder, true);
                    mChangeAnimations.remove(changeInfo.oldHolder);
                    dispatchFinishedWhenDone();
                }
            }).start();
        }
        if (newView != null) {
            final ViewPropertyAnimator newViewAnimation = newView.animate();
            mChangeAnimations.add(changeInfo.newHolder);
            newViewAnimation.translationX(0).translationY(0).setDuration(getChangeDuration()).alpha(1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animator) {
                    dispatchChangeStarting(changeInfo.newHolder, false);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    newViewAnimation.setListener(null);
                    newView.setAlpha(1);
                    newView.setTranslationX(0);
                    newView.setTranslationY(0);
                    dispatchChangeFinished(changeInfo.newHolder, false);
                    mChangeAnimations.remove(changeInfo.newHolder);
                    dispatchFinishedWhenDone();
                }
            }).start();
        }
    }

    private void endChangeAnimation(List<ChangeInfo> infoList, ViewHolder item) {
        for (int i = infoList.size() - 1; i >= 0; i--) {
            ChangeInfo changeInfo = infoList.get(i);
            if (endChangeAnimationIfNecessary(changeInfo, item)) {
                if (changeInfo.oldHolder == null && changeInfo.newHolder == null) {
                    infoList.remove(changeInfo);
                }
            }
        }
    }

    private void endChangeAnimationIfNecessary(ChangeInfo changeInfo) {
        if (changeInfo.oldHolder != null) {
            endChangeAnimationIfNecessary(changeInfo, changeInfo.oldHolder);
        }
        if (changeInfo.newHolder != null) {
            endChangeAnimationIfNecessary(changeInfo, changeInfo.newHolder);
        }
    }

    private boolean endChangeAnimationIfNecessary(ChangeInfo changeInfo, ViewHolder item) {
        boolean oldItem = false;
        if (changeInfo.newHolder == item) {
            changeInfo.newHolder = null;
        } else if (changeInfo.oldHolder == item) {
            changeInfo.oldHolder = null;
            oldItem = true;
        } else {
            return false;
        }
        item.itemView.setAlpha(1);
        item.itemView.setTranslationX(0);
        item.itemView.setTranslationY(0);
        dispatchChangeFinished(item, oldItem);
        return true;
    }

    @Override
    public void endAnimation(ViewHolder item) {
        final View view = item.itemView;
        // this will trigger end callback which should set properties to their target values.
        view.animate().cancel();
        for (int i = mPendingMoves.size() - 1; i >= 0; i--) {
            MoveInfo moveInfo = mPendingMoves.get(i);
            if (moveInfo.holder == item) {
                view.setTranslationY(0);
                view.setTranslationX(0);
                dispatchMoveFinished(item);
                mPendingMoves.remove(i);
            }
        }
        endChangeAnimation(mPendingChanges, item);
        if (mPendingRemovals.remove(item)) {
            view.setAlpha(1);
            dispatchRemoveFinished(item);
        }
        if (mPendingAdditions.remove(item)) {
            view.setAlpha(1);
            dispatchAddFinished(item);
        }

        for (int i = mChangesList.size() - 1; i >= 0; i--) {
            ArrayList<ChangeInfo> changes = mChangesList.get(i);
            endChangeAnimation(changes, item);
            if (changes.isEmpty()) {
                mChangesList.remove(i);
            }
        }
        for (int i = mMovesList.size() - 1; i >= 0; i--) {
            ArrayList<MoveInfo> moves = mMovesList.get(i);
            for (int j = moves.size() - 1; j >= 0; j--) {
                MoveInfo moveInfo = moves.get(j);
                if (moveInfo.holder == item) {
                    view.setTranslationY(0);
                    view.setTranslationX(0);
                    dispatchMoveFinished(item);
                    moves.remove(j);
                    if (moves.isEmpty()) {
                        mMovesList.remove(i);
                    }
                    break;
                }
            }
        }
        for (int i = mAdditionsList.size() - 1; i >= 0; i--) {
            ArrayList<ViewHolder> additions = mAdditionsList.get(i);
            if (additions.remove(item)) {
                view.setAlpha(1);
                dispatchAddFinished(item);
                if (additions.isEmpty()) {
                    mAdditionsList.remove(i);
                }
            }
        }

        mRemoveAnimations.remove(item);
        mAddAnimations.remove(item);
        mChangeAnimations.remove(item);
        mMoveAnimations.remove(item);

        dispatchFinishedWhenDone();
    }

    private void resetAnimation(ViewHolder holder) {
        holder.itemView.animate().setInterpolator(sDefaultInterpolator);
        endAnimation(holder);
    }

    @Override
    public boolean isRunning() {
        return (!mPendingAdditions.isEmpty()
                || !mPendingChanges.isEmpty()
                || !mPendingMoves.isEmpty()
                || !mPendingRemovals.isEmpty()
                || !mMoveAnimations.isEmpty()
                || !mRemoveAnimations.isEmpty()
                || !mAddAnimations.isEmpty()
                || !mChangeAnimations.isEmpty()
                || !mMovesList.isEmpty()
                || !mAdditionsList.isEmpty()
                || !mChangesList.isEmpty()
                || !mParentChangeAnimations.isEmpty()
        );
    }

    void dispatchFinishedWhenDone() {
        if (!isRunning()) {
            dispatchAnimationsFinished();
        }
    }

    @Override
    public void endAnimations() {
        int count = mPendingMoves.size();
        for (int i = count - 1; i >= 0; i--) {
            MoveInfo item = mPendingMoves.get(i);
            View view = item.holder.itemView;
            view.setTranslationY(0);
            view.setTranslationX(0);
            dispatchMoveFinished(item.holder);
            mPendingMoves.remove(i);
        }
        count = mPendingRemovals.size();
        for (int i = count - 1; i >= 0; i--) {
            ViewHolder item = mPendingRemovals.get(i);
            dispatchRemoveFinished(item);
            mPendingRemovals.remove(i);
        }
        count = mPendingAdditions.size();
        for (int i = count - 1; i >= 0; i--) {
            ViewHolder item = mPendingAdditions.get(i);
            item.itemView.setAlpha(1);
            dispatchAddFinished(item);
            mPendingAdditions.remove(i);
        }
        count = mPendingChanges.size();
        for (int i = count - 1; i >= 0; i--) {
            endChangeAnimationIfNecessary(mPendingChanges.get(i));
        }
        mPendingChanges.clear();
        if (!isRunning()) {
            return;
        }

        int listCount = mMovesList.size();
        for (int i = listCount - 1; i >= 0; i--) {
            ArrayList<MoveInfo> moves = mMovesList.get(i);
            count = moves.size();
            for (int j = count - 1; j >= 0; j--) {
                MoveInfo moveInfo = moves.get(j);
                ViewHolder item = moveInfo.holder;
                View view = item.itemView;
                view.setTranslationY(0);
                view.setTranslationX(0);
                dispatchMoveFinished(moveInfo.holder);
                moves.remove(j);
                if (moves.isEmpty()) {
                    mMovesList.remove(moves);
                }
            }
        }
        listCount = mAdditionsList.size();
        for (int i = listCount - 1; i >= 0; i--) {
            ArrayList<ViewHolder> additions = mAdditionsList.get(i);
            count = additions.size();
            for (int j = count - 1; j >= 0; j--) {
                ViewHolder item = additions.get(j);
                View view = item.itemView;
                view.setAlpha(1);
                dispatchAddFinished(item);
                additions.remove(j);
                if (additions.isEmpty()) {
                    mAdditionsList.remove(additions);
                }
            }
        }
        listCount = mChangesList.size();
        for (int i = listCount - 1; i >= 0; i--) {
            ArrayList<ChangeInfo> changes = mChangesList.get(i);
            count = changes.size();
            for (int j = count - 1; j >= 0; j--) {
                endChangeAnimationIfNecessary(changes.get(j));
                if (changes.isEmpty()) {
                    mChangesList.remove(changes);
                }
            }
        }

        for (ParentChangeInfo anim : mParentChangeAnimations) {
            anim.anim.cancel();
        }
        mParentChangeAnimations.clear();

        cancelAll(mRemoveAnimations);
        cancelAll(mMoveAnimations);
        cancelAll(mAddAnimations);
        cancelAll(mChangeAnimations);

        dispatchAnimationsFinished();
    }

    void cancelAll(List<ViewHolder> viewHolders) {
        for (int i = viewHolders.size() - 1; i >= 0; i--) {
            viewHolders.get(i).itemView.animate().cancel();
        }
    }

    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull ViewHolder viewHolder, @NonNull List<Object> payloads) {
        return !payloads.isEmpty() || super.canReuseUpdatedViewHolder(viewHolder, payloads);
    }

    private void startParentAnim() {
        if (!mParentChangeAnimations.isEmpty()) {
            ParentChangeInfo anim = mParentChangeAnimations.get(0);
            if (!anim.isCancel && !anim.anim.isStarted()) {
                anim.animatedObj.setProgress(0f);
                anim.anim.start();
            }
        }
    }

    private void parentChangeAnimImpl(int from, int to, int finalHeight, long duration) {
        final AnimatedObj parentAnimObj = new AnimatedObj(from, to, finalHeight);
        for (ParentChangeInfo mParentChangeAnimation : mParentChangeAnimations) {
            mParentChangeAnimation.anim.removeAllListeners();
            mParentChangeAnimation.anim.cancel();
            mParentChangeAnimation.isCancel = true;
        }
        mParentChangeAnimations.clear();
        ObjectAnimator anim = ObjectAnimator.ofFloat(parentAnimObj, "progress", 0f, 1f);
        ParentChangeInfo animationInfo = new ParentChangeInfo(anim, parentAnimObj);
        mParentChangeAnimations.add(animationInfo);
        anim.setDuration(duration);
        anim.setInterpolator(sDefaultInterpolator);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                anim.removeListener(this);
                parentAnimObj.setProgress(1f);
                mParentChangeAnimations.remove(animationInfo);
                dispatchFinishedWhenDone();
                parentAnimObj.onEnd();
            }
        });
    }

    private class AnimatedObj {
        public final int fromHeight;
        public final int targetHeight;
        public final int finalHeight;
        public int currentHeight;

        AnimatedObj(int fromHeight, int targetHeight, int finalHeight) {
            this.fromHeight = fromHeight;
            this.targetHeight = targetHeight;
            this.finalHeight = finalHeight;
            currentHeight = fromHeight;
        }

        @Keep
        public void setProgress(float progress) {
            currentHeight = (int) (progress * (targetHeight - fromHeight) + fromHeight);
            updateHeight(currentHeight);
        }

        public void onEnd() {
            currentHeight = targetHeight;
            updateHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private static int getViewMargin(View view) {
        if (view == null) {
            return 0;
        }
        int marginHeight = 0;
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) lp;
            marginHeight = marginLayoutParams.topMargin + marginLayoutParams.bottomMargin;
        }
        return marginHeight;
    }

    private static int getViewHeightWithMargins(View view) {
        int marginHeight = getViewMargin(view);
        return view.getHeight() + marginHeight;
    }
    
    private static void setLayoutParamsHeight(View view, int height) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.height = height;
        view.setLayoutParams(lp);
    }

    private void updateHeight(int height) {
        setLayoutParamsHeight(parent, height);
        recyclerView.setMinimumHeight(height);
    }

    private static void blockNextDraw(@NonNull View view) {
        ViewTreeObserver observer = view.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    ViewTreeObserver aliveObserver = observer;
                    if (!aliveObserver.isAlive()) {
                        aliveObserver = view.getViewTreeObserver();
                    }
                    if (aliveObserver.isAlive()) {
                        observer.removeOnPreDrawListener(this);
                        // 阻止下一次绘制，避免闪烁
                        return false;
                    }
                    return true;
                }
            });
        }
    }

    private void changeHeight(int delta) {
        updateHeight(delta + parent.getHeight());
    }

    private static int getViewHeightWithMargins(ViewHolder view) {
        if (view == null) {
            return 0;
        }
        return getViewHeightWithMargins(view.itemView);
    }

    @Override
    public long getChangeDuration() {
        if (DEBUG) {
            return 3000;
        }
        return super.getChangeDuration();
    }

    @Override
    public long getAddDuration() {
        if (DEBUG) {
            return 3000;
        }
        return super.getAddDuration();
    }

    @Override
    public long getRemoveDuration() {
        if (DEBUG) {
            return 3000;
        }
        return super.getRemoveDuration();
    }

    @Override
    public long getMoveDuration() {
        if (DEBUG) {
            return 3000;
        }
        return super.getMoveDuration();
    }
}

