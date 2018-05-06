package snakeeyes.recyclerg;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;


/**
 * Created by Barinov Maxim on 16.04.2018.
 */





public class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int VIEW_TYPE_ITEM = 0, VIEW_TYPE_LOADING = 1;
    private OnLoadMoreListener onLoadMoreListener;
    private boolean isLoading;
    private Context context;
    private ArrayList<CreateList> galeryList;
    private int visibleThreshold = 10;
    private int lastVisibleItem;
    private int totalItemCount;





    /** Объявляем масив с Createlist, передаем context, организовываем слушатель прокрутки ResyclerView
     *Прогружаем дополнительные картинки когда до конца уже прогруженых картинок их остается 10 и меньше
     */
    public MyAdapter(Context context, ArrayList<CreateList> galeryList, RecyclerView recyclerView) {
        this.galeryList = galeryList;
        this.context = context;

        final LinearLayoutManager linearLayoutManager
                = (LinearLayoutManager)recyclerView.getLayoutManager();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalItemCount = linearLayoutManager.getItemCount();
                lastVisibleItem = linearLayoutManager.findLastCompletelyVisibleItemPosition();

                if(!isLoading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                    if (onLoadMoreListener != null) {
                        onLoadMoreListener.onLoadMore();
                    }
                    isLoading = true;
                }
            }
        });
    }
    //возвращает 1 если нужно догрузить картинку и 0 если она уже загружена
    @Override
    public int getItemViewType(int position) {
        return galeryList.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    //обьявляем инерфейс для подгрузки новых картинок
    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    //передаем в recyclerview экран загрузки если идет загрузка
    //или саму картинку если загружена
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        if (i == VIEW_TYPE_ITEM) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cell_layout, viewGroup, false);
            return new ItemViewHolder(view);
        } else if (i == VIEW_TYPE_LOADING){
            View view = LayoutInflater.from(context).inflate(R.layout.item_loading, viewGroup, false);
            return new LoadingViewHolder(view);
        }
        return null;
    }

    //создаем обьекты загруженой картинки если есть и организовываем служатель нажатия на нее
    //иначе экрана загрузки
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int i) {
        if (viewHolder instanceof ItemViewHolder) {
            CreateList list = galeryList.get(i);
            ItemViewHolder itemHolder = (ItemViewHolder) viewHolder;
            //скачиваем и кешируем картинку из api
            Picasso.with(context).load(list.getImage_ID()).centerCrop()
                    .resize(400, 300).into(itemHolder.img);
            itemHolder.img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, ActivityFul.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("img", galeryList.get(i).getImage_ID());
                    context.startActivity(intent);
                }
            });
        }else {
            LoadingViewHolder loadingHolder = (LoadingViewHolder) viewHolder;
            loadingHolder.progressBar.setIndeterminate(true);
        }

    }

    //макет для картинки xml
    public class ItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView img;

        public ItemViewHolder(View view) {
            super(view);
            img = (ImageView) view.findViewById(R.id.img);
        }
    }

    //макет экрана загрузки xml
    public class LoadingViewHolder extends RecyclerView.ViewHolder {

        public ProgressBar progressBar;

        public LoadingViewHolder(View itemView) {
            super(itemView);
            progressBar = (ProgressBar)itemView.findViewById(R.id.progressBar);
        }
    }

    //если загружено - загрузка окончена
    public void setLoaded() {
        isLoading = false;
    }

    //передает размер масива с уже загружеными картинками
    @Override
    public int getItemCount() {
        return galeryList.size();
    }

    //для выявления на каком моменте произошла ошибока если таковая имеется
    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        int layoutPosition = holder.getLayoutPosition();
        Log.d(TAG, "onViewAttachedToWindow: getayoutPosition = " + layoutPosition);

        layoutPosition = holder.getAdapterPosition();
        Log.d(TAG, "onViewAttachedToWindow: getAdapterPosition = " + layoutPosition);

    }

}
